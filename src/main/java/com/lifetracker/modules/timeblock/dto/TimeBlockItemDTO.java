package com.lifetracker.modules.timeblock.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class TimeBlockItemDTO {

    @NotNull(message = "时间块索引不能为空")
    @Min(value = 0, message = "索引最小为0 (00:00~00:30)")
    @Max(value = 47, message = "索引最大为47 (23:30~24:00)")
    private Integer blockIndex;

    @NotBlank(message = "分类不能为空 (WORK/STUDY/SPORT/REST/SLEEP)")
    private String category;

    private String note;
}
