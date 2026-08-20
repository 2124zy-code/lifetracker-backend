package com.lifetracker.modules.timeblock.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeBlockDayVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDate date;

    private Integer totalRecordedBlocks;

    private Double totalFocusHours; // WORK + STUDY 的小时数

    private List<TimeBlockItemVO> blocks; // 完整的 48 格

    private List<TimeBlockCategoryStatVO> categoryStats; // 分类统计
}
