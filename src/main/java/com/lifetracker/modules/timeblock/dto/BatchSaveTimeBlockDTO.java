package com.lifetracker.modules.timeblock.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class BatchSaveTimeBlockDTO {

    @NotBlank(message = "记录日期不能为空 (YYYY-MM-DD)")
    private String date;

    @NotEmpty(message = "时间块列表不能为空")
    @Valid
    private List<TimeBlockItemDTO> blocks;
}
