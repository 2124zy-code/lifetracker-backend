package com.lifetracker.modules.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiReviewRadarVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 精力充沛度 (0~100)
     */
    private Integer energyVitality;

    /**
     * 深度专注力 (0~100)
     */
    private Integer deepFocus;

    /**
     * 作息规律度 (0~100)
     */
    private Integer scheduleRegularity;

    /**
     * 自律坚韧度 (0~100)
     */
    private Integer disciplineStreak;

    /**
     * 生活平衡度 (0~100)
     */
    private Integer lifeBalance;
}
