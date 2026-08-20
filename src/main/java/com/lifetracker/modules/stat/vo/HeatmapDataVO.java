package com.lifetracker.modules.stat.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapDataVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer year;

    private Integer totalActiveDays;

    private Integer maxStreak;

    private List<HeatmapDayVO> days;
}
