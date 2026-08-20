package com.lifetracker.modules.habit.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HabitVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String name;

    private String icon;

    private String color;

    private Integer targetDays;

    private Boolean isLoggedToday;

    private Integer currentStreak;

    private Integer totalLoggedDays;

    private Integer weekCompletedDays;

    private LocalDateTime createdAt;
}
