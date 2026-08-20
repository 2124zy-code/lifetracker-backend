package com.lifetracker.modules.stat.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自律能量指数 0 ~ 100
     */
    private Integer energyIndex;

    /**
     * 今日专注时长 (小时)
     */
    private Double todayFocusHours;

    /**
     * 今日习惯打卡进度 (如 3/4)
     */
    private Integer todayCompletedHabits;

    private Integer todayTotalHabits;

    /**
     * 历史累计总打卡数
     */
    private Integer totalLoggedCount;

    /**
     * 当前最长连击数 (天)
     */
    private Integer maxCurrentStreak;
}
