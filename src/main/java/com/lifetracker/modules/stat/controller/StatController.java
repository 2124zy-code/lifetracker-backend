package com.lifetracker.modules.stat.controller;

import com.lifetracker.common.Result;
import com.lifetracker.modules.stat.service.StatService;
import com.lifetracker.modules.stat.vo.HeatmapDataVO;
import com.lifetracker.modules.stat.vo.StatSummaryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/stat")
public class StatController {

    private final StatService statService;

    public StatController(StatService statService) {
        this.statService = statService;
    }

    @GetMapping("/heatmap")
    public Result<HeatmapDataVO> getHeatmapData(@RequestParam(required = false) Integer year) {
        HeatmapDataVO result = statService.getHeatmapData(year);
        return Result.success(result);
    }

    @GetMapping("/summary")
    public Result<StatSummaryVO> getStatSummary() {
        StatSummaryVO result = statService.getStatSummary();
        return Result.success(result);
    }
}
