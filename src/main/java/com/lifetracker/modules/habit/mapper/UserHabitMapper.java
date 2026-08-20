package com.lifetracker.modules.habit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifetracker.modules.habit.entity.UserHabit;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserHabitMapper extends BaseMapper<UserHabit> {
}
