package com.lifetracker.modules.timeblock.controller;

import com.lifetracker.common.Result;
import com.lifetracker.modules.timeblock.dto.BatchSaveTimeBlockDTO;
import com.lifetracker.modules.timeblock.service.TimeBlockService;
import com.lifetracker.modules.timeblock.vo.TimeBlockDayVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/timeblocks")
public class TimeBlockController {

    private final TimeBlockService timeBlockService;

    public TimeBlockController(TimeBlockService timeBlockService) {
        this.timeBlockService = timeBlockService;
    }

    @GetMapping
    public Result<TimeBlockDayVO> getDayTimeBlocks(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        TimeBlockDayVO result = timeBlockService.getDayTimeBlocks(date);
        return Result.success(result);
    }

    @PostMapping("/batch")
    public Result<TimeBlockDayVO> saveBatch(@Valid @RequestBody BatchSaveTimeBlockDTO dto) {
        log.info("[TimeBlockModule] 批量保存时间块请求: date={}, count={}", dto.getDate(), dto.getBlocks().size());
        TimeBlockDayVO result = timeBlockService.saveBatch(dto);
        return Result.success(result, "时间块更新成功");
    }
}
