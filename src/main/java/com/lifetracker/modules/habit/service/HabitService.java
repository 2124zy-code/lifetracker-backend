package com.lifetracker.modules.habit.service;

import com.lifetracker.modules.habit.dto.CreateHabitDTO;
import com.lifetracker.modules.habit.dto.ToggleHabitDTO;
import com.lifetracker.modules.habit.dto.UpdateHabitDTO;
import com.lifetracker.modules.habit.vo.HabitToggleVO;
import com.lifetracker.modules.habit.vo.HabitVO;

import java.time.LocalDate;
import java.util.List;

public interface HabitService {

    List<HabitVO> getHabitList(LocalDate targetDate);

    HabitVO createHabit(CreateHabitDTO dto);

    HabitVO updateHabit(Long habitId, UpdateHabitDTO dto);

    void deleteHabit(Long habitId);

    HabitToggleVO toggleHabit(Long habitId, ToggleHabitDTO dto);

    void injectDemoData();

    void resetDemoData();
}
