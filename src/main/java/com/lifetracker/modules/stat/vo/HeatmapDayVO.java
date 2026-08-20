package com.lifetracker.modules.stat.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapDayVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDate date;

    /**
     * 活跃度分值 0 ~ 100
     */
    private Integer score;

    /**
     * 发光色阶等级 0 ~ 4
     */
    private Integer level;

    /**
     * 当日完成打卡数
     */
    private Integer completedHabits;

    /**
     * 总习惯数
     */
    private Integer totalHabits;

    /**
     * 当日专注小时数
     */
    private Double focusHours;
}
