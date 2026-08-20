package com.lifetracker.modules.habit.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class CreateHabitDTO {

    @NotBlank(message = "习惯名称不能为空")
    @Size(max = 100, message = "习惯名称不能超过100个字符")
    private String name;

    private String icon;

    private String color;

    @Min(value = 1, message = "目标天数至少为1天")
    @Max(value = 7, message = "每周目标天数最多为7天")
    private Integer targetDays;
}
