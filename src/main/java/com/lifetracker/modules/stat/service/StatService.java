package com.lifetracker.modules.stat.service;

import com.lifetracker.modules.stat.vo.HeatmapDataVO;
import com.lifetracker.modules.stat.vo.StatSummaryVO;

public interface StatService {

    HeatmapDataVO getHeatmapData(Integer year);

    StatSummaryVO getStatSummary();
}
