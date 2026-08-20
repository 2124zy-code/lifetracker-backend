package com.lifetracker.modules.habit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifetracker.modules.habit.entity.HabitLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface HabitLogMapper extends BaseMapper<HabitLog> {

    @Select("SELECT * FROM habit_log WHERE user_id = #{userId} AND log_date BETWEEN #{startDate} AND #{endDate}")
    List<HabitLog> selectLogsInRange(@Param("userId") Long userId, 
                                     @Param("startDate") LocalDate startDate, 
                                     @Param("endDate") LocalDate endDate);
}
