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
public class TimeBlockCategoryStatVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String category;

    private String categoryName;

    private String color;

    private Integer blockCount;

    private Double totalHours;

    private Double percentage;
}
