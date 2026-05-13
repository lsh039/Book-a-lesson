package com.lsh.booking.mapper;

import com.lsh.booking.entity.Reservation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 预约表 Mapper 接口
 * </p>
 *
 * @author lsh
 * @since 2026-04-06
 */
@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {

    /*
    * 查询预约时间
    * */
    @Select("SELECT appointment_time, COUNT(*) as num " +
            "FROM reservation " +
            "WHERE appointment_time = #{date} AND course_id = #{courseId} " +
            "GROUP BY appointment_time")
    List<Map<String, Object>> countByTime(String date, Long courseId);
}
