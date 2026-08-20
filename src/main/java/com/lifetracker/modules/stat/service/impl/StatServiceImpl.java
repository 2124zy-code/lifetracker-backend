package com.lifetracker.modules.stat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lifetracker.common.UserContext;
import com.lifetracker.modules.habit.entity.HabitLog;
import com.lifetracker.modules.habit.entity.UserHabit;
import com.lifetracker.modules.habit.mapper.HabitLogMapper;
import com.lifetracker.modules.habit.mapper.UserHabitMapper;
import com.lifetracker.modules.stat.service.StatService;
import com.lifetracker.modules.stat.vo.HeatmapDataVO;
import com.lifetracker.modules.stat.vo.HeatmapDayVO;
import com.lifetracker.modules.stat.vo.StatSummaryVO;
import com.lifetracker.modules.timeblock.entity.UserTimeblock;
import com.lifetracker.modules.timeblock.mapper.UserTimeblockMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StatServiceImpl implements StatService {

    private final UserHabitMapper habitMapper;
    private final HabitLogMapper habitLogMapper;
    private final UserTimeblockMapper timeblockMapper;

    public StatServiceImpl(UserHabitMapper habitMapper,
                           HabitLogMapper habitLogMapper,
                           UserTimeblockMapper timeblockMapper) {
        this.habitMapper = habitMapper;
        this.habitLogMapper = habitLogMapper;
        this.timeblockMapper = timeblockMapper;
    }

    @Override
    public HeatmapDataVO getHeatmapData(Integer year) {
        Long userId = UserContext.getRequiredUserId();
        int queryYear = year != null ? year : Year.now().getValue();
        LocalDate startDate = LocalDate.of(queryYear, 1, 1);
        LocalDate endDate = LocalDate.of(queryYear, 12, 31);
        log.info("[StatModule] 聚合查询用户年度热力图: userId={}, year={}", userId, queryYear);

        // 1. 获取有效习惯总数
        Long totalHabitsCount = habitMapper.selectCount(new LambdaQueryWrapper<UserHabit>()
                .eq(UserHabit::getUserId, userId));
        int totalHabits = totalHabitsCount != null && totalHabitsCount > 0 ? totalHabitsCount.intValue() : 4;

        // 2. 单次区间覆盖查询打卡流水
        List<HabitLog> logs = habitLogMapper.selectLogsInRange(userId, startDate, endDate);
        Map<LocalDate, Long> dayHabitCountMap = logs.stream()
                .collect(Collectors.groupingBy(HabitLog::getLogDate, Collectors.counting()));

        // 3. 单次区间覆盖查询时间块
        List<UserTimeblock> timeblocks = timeblockMapper.selectBlocksInRange(userId, startDate, endDate);
        Map<LocalDate, List<UserTimeblock>> dayTimeblockMap = timeblocks.stream()
                .collect(Collectors.groupingBy(UserTimeblock::getRecordDate));

        // 4. 遍历全年自然日
        List<HeatmapDayVO> dayList = new ArrayList<>(366);
        int totalActiveDays = 0;
        int currentStreak = 0;
        int maxStreak = 0;

        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            int completedHabits = dayHabitCountMap.getOrDefault(cursor, 0L).intValue();
            List<UserTimeblock> dayBlocks = dayTimeblockMap.getOrDefault(cursor, Collections.emptyList());

            long focusBlockCount = dayBlocks.stream()
                    .filter(b -> "WORK".equalsIgnoreCase(b.getCategory()) || "STUDY".equalsIgnoreCase(b.getCategory()))
                    .count();
            double focusHours = focusBlockCount * 0.5;

            // 分值算法: Score = (当日打卡完成率 * 60) + (当日专注小时数 * 10)
            double completionRate = totalHabits > 0 ? (double) completedHabits / totalHabits : 0.0;
            int score = (int) Math.round((completionRate * 60) + (focusHours * 10));
            score = Math.max(0, Math.min(100, score));

            // 色阶映射
            int level;
            if (score == 0 && completedHabits == 0 && focusHours == 0) {
                level = 0;
            } else if (score <= 25) {
                level = 1;
            } else if (score <= 50) {
                level = 2;
            } else if (score <= 75) {
                level = 3;
            } else {
                level = 4;
            }

            if (score > 0) {
                totalActiveDays++;
                currentStreak++;
                if (currentStreak > maxStreak) {
                    maxStreak = currentStreak;
                }
            } else {
                currentStreak = 0;
            }

            dayList.add(HeatmapDayVO.builder()
                    .date(cursor)
                    .score(score)
                    .level(level)
                    .completedHabits(completedHabits)
                    .totalHabits(totalHabits)
                    .focusHours(focusHours)
                    .build());

            cursor = cursor.plusDays(1);
        }

        return HeatmapDataVO.builder()
                .year(queryYear)
                .totalActiveDays(totalActiveDays)
                .maxStreak(maxStreak)
                .days(dayList)
                .build();
    }

    @Override
    public StatSummaryVO getStatSummary() {
        Long userId = UserContext.getRequiredUserId();
        LocalDate today = LocalDate.now();
        log.info("[StatModule] 获取数据大盘摘要: userId={}, today={}", userId, today);

        // 1. 习惯总数与今日打卡数
        List<UserHabit> habits = habitMapper.selectList(new LambdaQueryWrapper<UserHabit>()
                .eq(UserHabit::getUserId, userId));
        int totalHabits = habits.size();

        List<HabitLog> todayLogs = habitLogMapper.selectList(new LambdaQueryWrapper<HabitLog>()
                .eq(HabitLog::getUserId, userId)
                .eq(HabitLog::getLogDate, today));
        int completedHabits = todayLogs.size();

        // 2. 今日专注时长
        List<UserTimeblock> todayBlocks = timeblockMapper.selectList(new LambdaQueryWrapper<UserTimeblock>()
                .eq(UserTimeblock::getUserId, userId)
                .eq(UserTimeblock::getRecordDate, today));
        long focusBlocks = todayBlocks.stream()
                .filter(b -> "WORK".equalsIgnoreCase(b.getCategory()) || "STUDY".equalsIgnoreCase(b.getCategory()))
                .count();
        double focusHours = focusBlocks * 0.5;

        // 3. 计算今日能量指数 (0~100)
        double habitFactor = totalHabits > 0 ? ((double) completedHabits / totalHabits) * 50.0 : 0.0;
        double focusFactor = Math.min(50.0, (focusHours / 6.0) * 50.0);
        int energyIndex = (int) Math.round(habitFactor + focusFactor);
        if (totalHabits == 0 && focusHours == 0) {
            energyIndex = 0;
        }

        // 4. 历史总打卡数
        Long totalLogCount = habitLogMapper.selectCount(new LambdaQueryWrapper<HabitLog>()
                .eq(HabitLog::getUserId, userId));

        // 5. 当前最长习惯连击
        int maxStreak = 0;
        for (UserHabit habit : habits) {
            int streak = calculateStreak(userId, habit.getId(), today);
            if (streak > maxStreak) {
                maxStreak = streak;
            }
        }

        return StatSummaryVO.builder()
                .energyIndex(energyIndex)
                .todayFocusHours(focusHours)
                .todayCompletedHabits(completedHabits)
                .todayTotalHabits(totalHabits)
                .totalLoggedCount(totalLogCount != null ? totalLogCount.intValue() : 0)
                .maxCurrentStreak(maxStreak)
                .build();
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
