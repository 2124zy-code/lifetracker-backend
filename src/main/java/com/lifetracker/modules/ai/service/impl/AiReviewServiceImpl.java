package com.lifetracker.modules.ai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lifetracker.common.UserContext;
import com.lifetracker.modules.ai.service.AiReviewService;
import com.lifetracker.modules.ai.service.LocalRuleReviewEngine;
import com.lifetracker.modules.ai.vo.AiReviewRadarVO;
import com.lifetracker.modules.ai.vo.AiReviewReportVO;
import com.lifetracker.modules.habit.entity.HabitLog;
import com.lifetracker.modules.habit.entity.UserHabit;
import com.lifetracker.modules.habit.mapper.HabitLogMapper;
import com.lifetracker.modules.habit.mapper.UserHabitMapper;
import com.lifetracker.modules.timeblock.entity.UserTimeblock;
import com.lifetracker.modules.timeblock.mapper.UserTimeblockMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
public class AiReviewServiceImpl implements AiReviewService {

    private final UserHabitMapper habitMapper;
    private final HabitLogMapper habitLogMapper;
    private final UserTimeblockMapper timeblockMapper;
    private final LocalRuleReviewEngine localRuleEngine;

    @Value("${ai.deepseek.api-url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${ai.deepseek.api-key:mock_key}")
    private String apiKey;

    @Value("${ai.deepseek.model:deepseek-chat}")
    private String model;

    @Value("${ai.timeout-seconds:6}")
    private int timeoutSeconds;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public AiReviewServiceImpl(UserHabitMapper habitMapper,
                               HabitLogMapper habitLogMapper,
                               UserTimeblockMapper timeblockMapper,
                               LocalRuleReviewEngine localRuleEngine) {
        this.habitMapper = habitMapper;
        this.habitLogMapper = habitLogMapper;
        this.timeblockMapper = timeblockMapper;
        this.localRuleEngine = localRuleEngine;
    }

    @Override
    public AiReviewReportVO generateWeeklyReview() {
        Long userId = UserContext.getRequiredUserId();
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);
        log.info("[AiModule] 开始生成过去7天精力周报: userId={}, range={} ~ {}", userId, startDate, today);

        // 1. 聚合过去 7 天打卡数据
        List<UserHabit> habits = habitMapper.selectList(new LambdaQueryWrapper<UserHabit>()
                .eq(UserHabit::getUserId, userId));
        int totalHabits = Math.max(1, habits.size());

        List<HabitLog> weekLogs = habitLogMapper.selectLogsInRange(userId, startDate, today);
        int totalLogCount = weekLogs.size();

        // 2. 聚合过去 7 天时间块数据
        List<UserTimeblock> weekBlocks = timeblockMapper.selectBlocksInRange(userId, startDate, today);
        double totalFocusHours = 0.0;
        double totalSleepHours = 0.0;
        double totalSportHours = 0.0;
        double totalRestHours = 0.0;

        for (UserTimeblock block : weekBlocks) {
            String cat = block.getCategory() != null ? block.getCategory().toUpperCase() : "EMPTY";
            if ("WORK".equals(cat) || "STUDY".equals(cat)) {
                totalFocusHours += 0.5;
            } else if ("SLEEP".equals(cat)) {
                totalSleepHours += 0.5;
            } else if ("SPORT".equals(cat)) {
                totalSportHours += 0.5;
            } else if ("REST".equals(cat)) {
                totalRestHours += 0.5;
            }
        }

        // 3. 计算当前最长习惯连击
        int maxStreak = 0;
        for (UserHabit habit : habits) {
            int streak = calculateStreak(userId, habit.getId(), today);
            if (streak > maxStreak) {
                maxStreak = streak;
            }
        }

        final double finalFocusHours = totalFocusHours;
        final double finalSleepHours = totalSleepHours;
        final double finalSportHours = totalSportHours;
        final double finalRestHours = totalRestHours;
        final int finalMaxStreak = maxStreak;

        // 4. 尝试异步调用大模型，配置 6 秒熔断断路器
        if (apiKey != null && !apiKey.contains("mock") && !apiKey.contains("demo")) {
            try {
                Future<AiReviewReportVO> future = executor.submit(() -> callLlmApi(
                        totalHabits, totalLogCount, finalFocusHours, finalSleepHours, finalSportHours, finalRestHours, finalMaxStreak
                ));
                AiReviewReportVO report = future.get(timeoutSeconds, TimeUnit.SECONDS);
                if (report != null) {
                    report.setIsFallback(false);
                    return report;
                }
            } catch (TimeoutException e) {
                log.warn("[AiModule] 云端大模型响应超过 {} 秒，触发自动降级熔断保护", timeoutSeconds);
            } catch (Exception e) {
                log.warn("[AiModule] 云端大模型调用异常: {}, 触发本地规则降级", e.getMessage());
            }
        }

        // 5. 规则降级引擎兜底保证 100% 毫秒级可用
        return localRuleEngine.generateRuleReport(
                totalHabits, totalLogCount, totalFocusHours, totalSleepHours, totalSportHours, totalRestHours, maxStreak
        );
    }

    private AiReviewReportVO callLlmApi(int habits, int logs, double focus, double sleep, double sport, double rest, int streak) {
        String prompt = String.format(
                "请你作为顶级精力管理与自律导师，根据用户过去7天的数据生成结构化周报JSON：\n" +
                "- 习惯总数：%d，过去7天打卡次数：%d\n" +
                "- 累计深度专注时长：%.1f小时\n" +
                "- 累计睡眠时长：%.1f小时\n" +
                "- 累计运动时长：%.1f小时\n" +
                "- 当前最长连击天数：%d天\n\n" +
                "请严格只返回以下格式的合法JSON：\n" +
                "{\n" +
                "  \"overallScore\": 88,\n" +
                "  \"diagnosisTitle\": \"高能极客自律掌控者\",\n" +
                "  \"summary\": \"一句话深度总结\",\n" +
                "  \"radar\": {\"energyVitality\": 85, \"deepFocus\": 90, \"scheduleRegularity\": 80, \"disciplineStreak\": 88, \"lifeBalance\": 75},\n" +
                "  \"highlights\": [\"优势1\", \"优势2\", \"优势3\"],\n" +
                "  \"bottlenecks\": [\"痛点1\", \"痛点2\"],\n" +
                "  \"actionRecommendations\": [\"行动1\", \"行动2\", \"行动3\"]\n" +
                "}", habits, logs, focus, sleep, sport, streak);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        RestTemplate restTemplate = new RestTemplate(factory);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        
        Map<String, String> msg = new HashMap<>();
        msg.put("role", "user");
        msg.put("content", prompt);
        body.put("messages", Collections.singletonList(msg));
        
        Map<String, String> formatMap = new HashMap<>();
        formatMap.put("type", "json_object");
        body.put("response_format", formatMap);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JSONObject root = JSON.parseObject(response.getBody());
            String content = root.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

            JSONObject json = JSON.parseObject(content);
            AiReviewRadarVO radar = json.getObject("radar", AiReviewRadarVO.class);

            return AiReviewReportVO.builder()
                    .overallScore(json.getInteger("overallScore"))
                    .diagnosisTitle(json.getString("diagnosisTitle"))
                    .summary(json.getString("summary"))
                    .radar(radar)
                    .highlights(json.getList("highlights", String.class))
                    .bottlenecks(json.getList("bottlenecks", String.class))
                    .actionRecommendations(json.getList("actionRecommendations", String.class))
                    .isFallback(false)
                    .generatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .build();
        }
        return null;
    }

    private int calculateStreak(Long userId, Long habitId, LocalDate baseDate) {
        int streak = 0;
        LocalDate cursor = baseDate;
        if (!hasLogged(userId, habitId, cursor)) {
            cursor = baseDate.minusDays(1);
        }
        while (hasLogged(userId, habitId, cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
            if (streak > 365) break;
        }
        return streak;
    }

    private boolean hasLogged(Long userId, Long habitId, LocalDate date) {
        Long count = habitLogMapper.selectCount(new LambdaQueryWrapper<HabitLog>()
                .eq(HabitLog::getUserId, userId)
                .eq(HabitLog::getHabitId, habitId)
                .eq(HabitLog::getLogDate, date));
        return count != null && count > 0;
    }
}
