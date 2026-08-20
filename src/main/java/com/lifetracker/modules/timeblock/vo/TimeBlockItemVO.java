package com.lifetracker.modules.timeblock.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeBlockItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer blockIndex;

    private String timeRange; // 如 "09:00 - 09:30"

    private String category; // WORK, STUDY, SPORT, REST, SLEEP, EMPTY

    private String categoryName; // 深度工作, 学习充电, 运动健身, 休闲放松, 睡眠休息, 未分配

    private String color; // #8B5CF6, #3B82F6, #10B981, #F59E0B, #6366F1, #1E293B

    private String note;
}
