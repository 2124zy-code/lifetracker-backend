package com.lifetracker.modules.timeblock.service;

import com.lifetracker.modules.timeblock.dto.BatchSaveTimeBlockDTO;
import com.lifetracker.modules.timeblock.vo.TimeBlockDayVO;

import java.time.LocalDate;

public interface TimeBlockService {

    TimeBlockDayVO getDayTimeBlocks(LocalDate date);

    TimeBlockDayVO saveBatch(BatchSaveTimeBlockDTO dto);
}
