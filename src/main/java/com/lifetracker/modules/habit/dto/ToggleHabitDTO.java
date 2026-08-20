package com.lifetracker.modules.habit.dto;

import lombok.Data;

@Data
public class ToggleHabitDTO {

    // 格式: YYYY-MM-DD，若为空则默认当前自然日
    private String logDate;
}
