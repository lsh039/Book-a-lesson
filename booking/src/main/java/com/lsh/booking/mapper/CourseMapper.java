package com.lsh.booking.mapper;

import com.lsh.booking.entity.Course;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;

/**
 * <p>
 * 课程/服务表 Mapper 接口
 * </p>
 *
 * @author lsh
 * @since 2026-04-06
 */
@Mapper
public interface CourseMapper extends BaseMapper<Course> {

}
