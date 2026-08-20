package com.lifetracker.modules.habit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lifetracker.common.BusinessException;
import com.lifetracker.common.ErrorCodeEnum;
import com.lifetracker.common.UserContext;
import com.lifetracker.modules.habit.dto.CreateHabitDTO;
import com.lifetracker.modules.habit.dto.ToggleHabitDTO;
import com.lifetracker.modules.habit.dto.UpdateHabitDTO;
import com.lifetracker.modules.habit.entity.HabitLog;
import com.lifetracker.modules.habit.entity.UserHabit;
import com.lifetracker.modules.habit.mapper.HabitLogMapper;
import com.lifetracker.modules.habit.mapper.UserHabitMapper;
import com.lifetracker.modules.habit.service.HabitService;
import com.lifetracker.modules.habit.vo.HabitToggleVO;
import com.lifetracker.modules.habit.vo.HabitVO;
import com.lifetracker.modules.timeblock.entity.UserTimeblock;
import com.lifetracker.modules.timeblock.mapper.UserTimeblockMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HabitServiceImpl implements HabitService {

    private final UserHabitMapper habitMapper;
    private final HabitLogMapper habitLogMapper;
    private final UserTimeblockMapper timeblockMapper;

    public HabitServiceImpl(UserHabitMapper habitMapper, 
                            HabitLogMapper habitLogMapper, 
                            UserTimeblockMapper timeblockMapper) {
        this.habitMapper = habitMapper;
        this.habitLogMapper = habitLogMapper;
        this.timeblockMapper = timeblockMapper;
    }

    @Override
    public List<HabitVO> getHabitList(LocalDate targetDate) {
        Long userId = UserContext.getRequiredUserId();
        LocalDate queryDate = targetDate != null ? targetDate : LocalDate.now();
        log.info("[HabitModule] 获取用户习惯列表: userId={}, queryDate={}", userId, queryDate);

        List<UserHabit> habits = habitMapper.selectList(new LambdaQueryWrapper<UserHabit>()
                .eq(UserHabit::getUserId, userId)
                .orderByDesc(UserHabit::getId));

        if (habits.isEmpty()) {
            return Collections.emptyList();
        }

        // 查询今日打卡记录
        List<HabitLog> todayLogs = habitLogMapper.selectList(new LambdaQueryWrapper<HabitLog>()
                .eq(HabitLog::getUserId, userId)
                .eq(HabitLog::getLogDate, queryDate));
        Set<Long> loggedHabitIds = todayLogs.stream().map(HabitLog::getHabitId).collect(Collectors.toSet());

        // 计算本周范围
        LocalDate monday = queryDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = queryDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        List<HabitLog> weekLogs = habitLogMapper.selectLogsInRange(userId, monday, sunday);

        // 历史打卡总数
        List<HabitLog> allLogs = habitLogMapper.selectList(new LambdaQueryWrapper<HabitLog>()
                .eq(HabitLog::getUserId, userId));
        Map<Long, Long> totalLogCountMap = allLogs.stream()
                .collect(Collectors.groupingBy(HabitLog::getHabitId, Collectors.counting()));

        // 构建 VO
        List<HabitVO> resultList = new ArrayList<>();
        for (UserHabit habit : habits) {
            boolean isLoggedToday = loggedHabitIds.contains(habit.getId());
            int streak = calculateStreak(userId, habit.getId(), queryDate);

            long weekCompletedDays = weekLogs.stream()
                    .filter(log -> log.getHabitId().equals(habit.getId()))
                    .map(HabitLog::getLogDate)
                    .distinct()
                    .count();

            long totalLoggedDays = totalLogCountMap.getOrDefault(habit.getId(), 0L);

            resultList.add(HabitVO.builder()
                    .id(habit.getId())
                    .userId(habit.getUserId())
                    .name(habit.getName())
                    .icon(habit.getIcon())
                    .color(habit.getColor())
                    .targetDays(habit.getTargetDays() != null ? habit.getTargetDays() : 7)
                    .isLoggedToday(isLoggedToday)
                    .currentStreak(streak)
                    .totalLoggedDays((int) totalLoggedDays)
                    .weekCompletedDays((int) weekCompletedDays)
                    .createdAt(habit.getCreatedAt())
                    .build());
        }

        return resultList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HabitVO createHabit(CreateHabitDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        log.info("[HabitModule] 创建新习惯: userId={}, name={}", userId, dto.getName());

        UserHabit habit = UserHabit.builder()
                .userId(userId)
                .name(dto.getName().trim())
                .icon(StringUtils.hasText(dto.getIcon()) ? dto.getIcon().trim() : "🔥")
                .color(StringUtils.hasText(dto.getColor()) ? dto.getColor().trim() : "#10B981")
                .targetDays(dto.getTargetDays() != null ? dto.getTargetDays() : 7)
                .isDeleted(0)
                .createdAt(LocalDateTime.now())
                .build();

        habitMapper.insert(habit);

        return HabitVO.builder()
                .id(habit.getId())
                .userId(habit.getUserId())
                .name(habit.getName())
                .icon(habit.getIcon())
                .color(habit.getColor())
                .targetDays(habit.getTargetDays())
                .isLoggedToday(false)
                .currentStreak(0)
                .totalLoggedDays(0)
                .weekCompletedDays(0)
                .createdAt(habit.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HabitVO updateHabit(Long habitId, UpdateHabitDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        log.info("[HabitModule] 更新习惯配置: userId={}, habitId={}", userId, habitId);

        UserHabit habit = habitMapper.selectOne(new LambdaQueryWrapper<UserHabit>()
                .eq(UserHabit::getId, habitId)
                .eq(UserHabit::getUserId, userId));
        if (habit == null) {
            throw new BusinessException(ErrorCodeEnum.HABIT_NOT_FOUND);
        }

        habit.setName(dto.getName().trim());
        if (StringUtils.hasText(dto.getIcon())) habit.setIcon(dto.getIcon().trim());
        if (StringUtils.hasText(dto.getColor())) habit.setColor(dto.getColor().trim());
        if (dto.getTargetDays() != null) habit.setTargetDays(dto.getTargetDays());

        habitMapper.updateById(habit);

        return HabitVO.builder()
                .id(habit.getId())
                .userId(habit.getUserId())
                .name(habit.getName())
                .icon(habit.getIcon())
                .color(habit.getColor())
                .targetDays(habit.getTargetDays())
                .isLoggedToday(false)
                .currentStreak(calculateStreak(userId, habit.getId(), LocalDate.now()))
                .createdAt(habit.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHabit(Long habitId) {
        Long userId = UserContext.getRequiredUserId();
        log.info("[HabitModule] 软删除习惯: userId={}, habitId={}", userId, habitId);

        UserHabit habit = habitMapper.selectOne(new LambdaQueryWrapper<UserHabit>()
                .eq(UserHabit::getId, habitId)
                .eq(UserHabit::getUserId, userId));
        if (habit == null) {
            throw new BusinessException(ErrorCodeEnum.HABIT_NOT_FOUND);
        }

        habitMapper.deleteById(habitId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HabitToggleVO toggleHabit(Long habitId, ToggleHabitDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        LocalDate targetDate = LocalDate.now();
        if (dto != null && StringUtils.hasText(dto.getLogDate())) {
            try {
                targetDate = LocalDate.parse(dto.getLogDate());
            } catch (Exception e) {
                log.warn("[HabitModule] 日期格式无效: logDate={}", dto.getLogDate());
                throw new BusinessException(ErrorCodeEnum.PARAM_INVALID, "打卡日期格式错误，应为 YYYY-MM-DD");
            }
        }

        log.info("[HabitModule] 触发打卡Toggle: userId={}, habitId={}, date={}", userId, habitId, targetDate);

        // 校验习惯归属
        UserHabit habit = habitMapper.selectOne(new LambdaQueryWrapper<UserHabit>()
                .eq(UserHabit::getId, habitId)
                .eq(UserHabit::getUserId, userId));
        if (habit == null) {
            throw new BusinessException(ErrorCodeEnum.HABIT_NOT_FOUND);
        }

        // 检查打卡状态
        HabitLog existLog = habitLogMapper.selectOne(new LambdaQueryWrapper<HabitLog>()
                .eq(HabitLog::getUserId, userId)
                .eq(HabitLog::getHabitId, habitId)
                .eq(HabitLog::getLogDate, targetDate));

        boolean nowLogged;
        if (existLog != null) {
            // 已打卡 -> 取消打卡
            habitLogMapper.deleteById(existLog.getId());
            nowLogged = false;
            log.info("[HabitModule] 取消打卡成功: habitId={}, date={}", habitId, targetDate);
        } else {
            // 未打卡 -> 插入打卡
            try {
                HabitLog newLog = HabitLog.builder()
                        .userId(userId)
                        .habitId(habitId)
                        .logDate(targetDate)
                        .score(1)
                        .createdAt(LocalDateTime.now())
                        .build();
                habitLogMapper.insert(newLog);
                nowLogged = true;
                log.info("[HabitModule] 打卡成功: habitId={}, date={}", habitId, targetDate);
            } catch (DuplicateKeyException e) {
                // 并发重复打卡幂等捕获
                log.warn("[HabitModule] 唯一索引冲突捕获，幂等返回已打卡状态: habitId={}, date={}", habitId, targetDate);
                nowLogged = true;
            }
        }

        int streak = calculateStreak(userId, habitId, targetDate);

        return HabitToggleVO.builder()
                .habitId(habitId)
                .isLogged(nowLogged)
                .currentStreak(streak)
                .message(nowLogged ? "打卡成功！能量充能中" : "已取消今日打卡")
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void injectDemoData() {
        Long userId = UserContext.getRequiredUserId();
        log.info("[HabitModule] 为用户注入 30 天丰富演示数据: userId={}", userId);

        List<UserHabit> habits = habitMapper.selectList(new LambdaQueryWrapper<UserHabit>()
                .eq(UserHabit::getUserId, userId));
        if (habits.isEmpty()) {
            // 初始化默认习惯
            UserHabit h1 = UserHabit.builder().userId(userId).name("早起晨光唤醒 (06:30)").icon("🌅").color("#10B981").targetDays(7).isDeleted(0).createdAt(LocalDateTime.now()).build();
            UserHabit h2 = UserHabit.builder().userId(userId).name("深度专注工作 4h").icon("💻").color("#8B5CF6").targetDays(5).isDeleted(0).createdAt(LocalDateTime.now()).build();
            UserHabit h3 = UserHabit.builder().userId(userId).name("硬核健身/有氧 45m").icon("🏋️").color("#F59E0B").targetDays(4).isDeleted(0).createdAt(LocalDateTime.now()).build();
            UserHabit h4 = UserHabit.builder().userId(userId).name("睡前阅读与冥想 30m").icon("📖").color("#3B82F6").targetDays(7).isDeleted(0).createdAt(LocalDateTime.now()).build();
            habitMapper.insert(h1);
            habitMapper.insert(h2);
            habitMapper.insert(h3);
            habitMapper.insert(h4);
            habits = Arrays.asList(h1, h2, h3, h4);
        }

        LocalDate today = LocalDate.now();
        Random random = new Random(42);

        // 注入过去 30 天打卡记录
        for (int dayOffset = 30; dayOffset >= 0; dayOffset--) {
            LocalDate logDate = today.minusDays(dayOffset);
            for (UserHabit habit : habits) {
                // 85% 几率打卡，创造高质量热力图和连击
                if (random.nextDouble() < 0.85) {
                    try {
                        HabitLog logEntity = HabitLog.builder()
                                .userId(userId)
                                .habitId(habit.getId())
                                .logDate(logDate)
                                .score(1)
                                .createdAt(LocalDateTime.now())
                                .build();
                        habitLogMapper.insert(logEntity);
                    } catch (DuplicateKeyException ignored) {
                    }
                }
            }

            // 注入过去 7 天的 48 格时间块数据
            if (dayOffset <= 7) {
                for (int block = 0; block < 48; block++) {
                    String category;
                    String note = "";
                    if (block >= 0 && block < 14) { // 00:00 ~ 07:00
                        category = "SLEEP";
                        note = "深度睡眠与休息";
                    } else if (block >= 14 && block < 17) { // 07:00 ~ 08:30
                        category = "REST";
                        note = "晨间唤醒与早餐";
                    } else if (block >= 17 && block < 24) { // 08:30 ~ 12:00
                        category = "WORK";
                        note = "系统核心架构研发";
                    } else if (block >= 24 && block < 27) { // 12:00 ~ 13:30
                        category = "REST";
                        note = "午餐与午休充能";
                    } else if (block >= 27 && block < 36) { // 13:30 ~ 18:00
                        category = "WORK";
                        note = "代码重构与前沿技术攻坚";
                    } else if (block >= 36 && block < 39) { // 18:00 ~ 19:30
                        category = "SPORT";
                        note = "有氧跑步与硬核力量训练";
                    } else if (block >= 39 && block < 44) { // 19:30 ~ 22:00
                        category = "STUDY";
                        note = "AI 大模型架构进阶研读";
                    } else { // 22:00 ~ 24:00
                        category = "REST";
                        note = "复盘与睡前准备";
                    }

                    try {
                        UserTimeblock tb = UserTimeblock.builder()
                                .userId(userId)
                                .recordDate(logDate)
                                .blockIndex(block)
                                .category(category)
                                .note(note)
                                .createdAt(LocalDateTime.now())
                                .build();
                        timeblockMapper.insert(tb);
                    } catch (DuplicateKeyException ignored) {
                    }
                }
            }
        }
        log.info("[HabitModule] 演示数据注入完成: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetDemoData() {
        Long userId = UserContext.getRequiredUserId();
        log.info("[HabitModule] 清空用户所有打卡与时间块数据: userId={}", userId);

        habitLogMapper.delete(new LambdaQueryWrapper<HabitLog>().eq(HabitLog::getUserId, userId));
        timeblockMapper.delete(new LambdaQueryWrapper<UserTimeblock>().eq(UserTimeblock::getUserId, userId));
        log.info("[HabitModule] 数据清空完成: userId={}", userId);
    }

    /**
     * 严格模式 Streak 算法
     */
    private int calculateStreak(Long userId, Long habitId, LocalDate baseDate) {
        int streak = 0;
        LocalDate cursor = baseDate;

        boolean todayLogged = hasLogged(userId, habitId, cursor);
        if (!todayLogged) {
            cursor = baseDate.minusDays(1);
        }

        while (hasLogged(userId, habitId, cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
            if (streak > 365) break; // 防死循环
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
