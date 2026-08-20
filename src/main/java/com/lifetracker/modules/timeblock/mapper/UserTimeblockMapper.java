package com.lifetracker.modules.timeblock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifetracker.modules.timeblock.entity.UserTimeblock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface UserTimeblockMapper extends BaseMapper<UserTimeblock> {

    @Select("SELECT * FROM user_timeblock WHERE user_id = #{userId} AND record_date BETWEEN #{startDate} AND #{endDate}")
    List<UserTimeblock> selectBlocksInRange(@Param("userId") Long userId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);
}
