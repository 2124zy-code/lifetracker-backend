package com.lifetracker.modules.ai.vo;

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
public class AiReviewReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 综合自律指数 (0~100)
     */
    private Integer overallScore;

    /**
     * 诊断头衔 (如: "高能极客自律领航者")
     */
    private String diagnosisTitle;

    /**
     * 核心总结
     */
    private String summary;

    /**
     * 五维精力雷达数据
     */
    private AiReviewRadarVO radar;

    /**
     * 核心优势高光 (3条)
     */
    private List<String> highlights;

    /**
     * 风险与精力瓶颈预警 (2条)
     */
    private List<String> bottlenecks;

    /**
     * 下周落地执行建议 (3条)
     */
    private List<String> actionRecommendations;

    /**
     * 是否触发了本地专家规则降级
     */
    private Boolean isFallback;

    private String generatedAt;
}
