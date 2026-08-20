package com.lifetracker.modules.habit.controller;

import com.lifetracker.common.Result;
import com.lifetracker.modules.habit.dto.CreateHabitDTO;
import com.lifetracker.modules.habit.dto.ToggleHabitDTO;
import com.lifetracker.modules.habit.dto.UpdateHabitDTO;
import com.lifetracker.modules.habit.service.HabitService;
import com.lifetracker.modules.habit.vo.HabitToggleVO;
import com.lifetracker.modules.habit.vo.HabitVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/habits")
public class HabitController {

    private final HabitService habitService;

    public HabitController(HabitService habitService) {
        this.habitService = habitService;
    }

    @GetMapping
    public Result<List<HabitVO>> getHabitList(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<HabitVO> list = habitService.getHabitList(date);
        return Result.success(list);
    }

    @PostMapping
    public Result<HabitVO> createHabit(@Valid @RequestBody CreateHabitDTO dto) {
        log.info("[HabitModule] 创建习惯接口调用: name={}", dto.getName());
        HabitVO result = habitService.createHabit(dto);
        return Result.success(result, "创建习惯成功");
    }

    @PutMapping("/{id}")
    public Result<HabitVO> updateHabit(@PathVariable("id") Long id, @Valid @RequestBody UpdateHabitDTO dto) {
        log.info("[HabitModule] 更新习惯接口调用: id={}", id);
        HabitVO result = habitService.updateHabit(id, dto);
        return Result.success(result, "更新习惯成功");
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteHabit(@PathVariable("id") Long id) {
        log.info("[HabitModule] 删除习惯接口调用: id={}", id);
        habitService.deleteHabit(id);
        return Result.success(null, "删除习惯成功");
    }

    @PostMapping("/{id}/toggle")
    public Result<HabitToggleVO> toggleHabit(
            @PathVariable("id") Long id,
            @RequestBody(required = false) ToggleHabitDTO dto) {
        log.info("[HabitModule] 打卡Toggle接口调用: id={}", id);
        HabitToggleVO result = habitService.toggleHabit(id, dto);
        return Result.success(result, result.getMessage());
    }

    @PostMapping("/demo/inject")
    public Result<Void> injectDemoData() {
        log.info("[HabitModule] 请求注入 30 天演示数据");
        habitService.injectDemoData();
        return Result.success(null, "30天演示数据注入成功！请查看热力图与统计");
    }

    @PostMapping("/demo/reset")
    public Result<Void> resetDemoData() {
        log.info("[HabitModule] 请求重置清空数据");
        habitService.resetDemoData();
        return Result.success(null, "数据已成功清空");
    }
}
