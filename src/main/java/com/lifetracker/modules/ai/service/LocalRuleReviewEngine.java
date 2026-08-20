package com.lifetracker.modules.ai.service;

import com.lifetracker.modules.ai.vo.AiReviewRadarVO;
import com.lifetracker.modules.ai.vo.AiReviewReportVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class LocalRuleReviewEngine {

    public AiReviewReportVO generateRuleReport(int totalHabitCount,
                                               int loggedHabitCountIn7Days,
                                               double totalFocusHoursIn7Days,
                                               double totalSleepHoursIn7Days,
                                               double totalSportHoursIn7Days,
                                               double totalRestHoursIn7Days,
                                               int maxStreak) {
        log.info("[AiModule] 启动本地专家规则诊断引擎: habits={}, logs={}, focusHours={}", 
                totalHabitCount, loggedHabitCountIn7Days, totalFocusHoursIn7Days);

        int maxPossibleLogs = Math.max(1, totalHabitCount * 7);
        double habitRate = Math.min(1.0, (double) loggedHabitCountIn7Days / maxPossibleLogs);
        double avgDailyFocus = totalFocusHoursIn7Days / 7.0;
        double avgDailySleep = totalSleepHoursIn7Days / 7.0;
        double avgDailySport = totalSportHoursIn7Days / 7.0;

        // 计算 5 维雷达指标
        int deepFocus = Math.min(100, (int) Math.round((avgDailyFocus / 5.0) * 100));
        deepFocus = Math.max(30, deepFocus);

        int scheduleRegularity = Math.min(100, (int) Math.round((avgDailySleep >= 6.5 && avgDailySleep <= 8.5 ? 92 : 68)));
        
        int disciplineStreak = Math.min(100, (int) Math.round((habitRate * 70) + Math.min(30, maxStreak * 5)));
        disciplineStreak = Math.max(20, disciplineStreak);

        int energyVitality = (int) Math.round((avgDailySport * 35) + (scheduleRegularity * 0.4) + (deepFocus * 0.25));
        energyVitality = Math.min(100, Math.max(35, energyVitality));

        int lifeBalance = (int) Math.round((totalRestHoursIn7Days > 0 ? 80 : 50) + (avgDailySport > 0.3 ? 15 : 0));
        lifeBalance = Math.min(100, Math.max(30, lifeBalance));

        int overallScore = (int) Math.round((deepFocus * 0.25) + (disciplineStreak * 0.3) + (scheduleRegularity * 0.15) + (energyVitality * 0.15) + (lifeBalance * 0.15));
        overallScore = Math.min(100, Math.max(10, overallScore));

        // 评级头衔与核心评价
        String diagnosisTitle;
        String summary;
        if (overallScore >= 85) {
            diagnosisTitle = "高能极客自律领航者 ⚡";
            summary = String.format("过去 7 天展现出极高水准的时间掌控力！日均专注时长达 %.1f 小时，习惯履约率高达 %.0f%%，心流沉浸与精力节奏高度契合。", avgDailyFocus, habitRate * 100);
        } else if (overallScore >= 70) {
            diagnosisTitle = "稳健进阶自律探索者 🚀";
            summary = String.format("自律节奏稳步上升！过去 7 天累计打卡 %d 次，日均专注 %.1f 小时。核心业务推进稳定，但在作息与运动恢复维度仍有进阶空间。", loggedHabitCountIn7Days, avgDailyFocus);
        } else {
            diagnosisTitle = "觉醒突破自律筑基期 🌱";
            summary = String.format("过去 7 天精力存在碎片化分布倾向，建议从 1~2 个原子习惯切入，优先建立每日固定专注时间块，重构正向反馈飞轮。");
        }

        // 高光优势
        List<String> highlights = new ArrayList<>();
        if (avgDailyFocus >= 3.0) {
            highlights.add(String.format("深度专注表现突出：过去 7 天累计高能工作/学习达 %.1f 小时，心流产出充沛。", totalFocusHoursIn7Days));
        } else {
            highlights.add("具备明确的目标意识，打卡框架已初步建立并开始沉淀行为数据。");
        }
        if (maxStreak >= 5) {
            highlights.add(String.format("连击坚韧度可观：当前习惯最高连击达到 %d 天，形成了坚实的心理惯性。", maxStreak));
        } else {
            highlights.add("积极尝试建立个人打卡仪式感，行为触点正在逐步固化。");
        }
        if (avgDailySleep >= 6.5) {
            highlights.add("作息基线保持健康，睡眠恢复区间稳定，为高强度心流提供了生理能量底座。");
        } else {
            highlights.add("时间块分类覆盖完整，清晰呈现了多维生活的流向分布。");
        }

        // 风险与瓶颈
        List<String> bottlenecks = new ArrayList<>();
        if (avgDailySport < 0.3) {
            bottlenecks.add("运动与身体充能时间偏低，久坐可能导致下半周脑力疲劳累积。");
        } else {
            bottlenecks.add("偶有时间碎片化情况，注意防范突发琐事打断深度心流。");
        }
        if (habitRate < 0.7) {
            bottlenecks.add("部分习惯存在间歇性断签，可适当降低单次执行摩擦力（如拆分为微习惯）。");
        } else {
            bottlenecks.add("夜晚休闲与屏幕时间需适当节制，防止入睡延迟影响次日晨间峰值精力。");
        }

        // 落地建议
        List<String> actions = new ArrayList<>();
        actions.add("锁定晨间 90 分钟黄金专注时段（09:00~10:30），杜绝任何即时通讯工具干扰，完成当天最核心的重度任务。");
        actions.add("采用 25m+5m 番茄时间块切分，并在傍晚安排 20~30 分钟有氧或拉伸，激活脑源性神经生长因子（BDNF）。");
        actions.add("严格执行断签归零前的守护机制，遇不可抗力时以最小可行剂量（如阅读1页、深蹲10次）捍卫 Streak 连击。");

        AiReviewRadarVO radar = AiReviewRadarVO.builder()
                .energyVitality(energyVitality)
                .deepFocus(deepFocus)
                .scheduleRegularity(scheduleRegularity)
                .disciplineStreak(disciplineStreak)
                .lifeBalance(lifeBalance)
                .build();

        return AiReviewReportVO.builder()
                .overallScore(overallScore)
                .diagnosisTitle(diagnosisTitle)
                .summary(summary)
                .radar(radar)
                .highlights(highlights)
                .bottlenecks(bottlenecks)
                .actionRecommendations(actions)
                .isFallback(true)
                .generatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .build();
    }
}
