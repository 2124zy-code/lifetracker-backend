package com.lifetracker.modules.ai.controller;

import com.lifetracker.common.Result;
import com.lifetracker.modules.ai.service.AiReviewService;
import com.lifetracker.modules.ai.vo.AiReviewReportVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/ai")
public class AiReviewController {

    private final AiReviewService aiReviewService;

    public AiReviewController(AiReviewService aiReviewService) {
        this.aiReviewService = aiReviewService;
    }

    @PostMapping("/review")
    public Result<AiReviewReportVO> generateWeeklyReview() {
        log.info("[AiModule] 收到生成周度精力复盘报告请求");
        AiReviewReportVO report = aiReviewService.generateWeeklyReview();
        return Result.success(report, "AI精力复盘报告已生成");
    }
}
