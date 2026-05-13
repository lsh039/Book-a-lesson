package com.lsh.booking.service.impl;

import com.lsh.booking.entity.Course;
import com.lsh.booking.mapper.CourseMapper;
import com.lsh.booking.service.ICourseService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 课程/服务表 服务实现类
 * </p>
 *
 * @author lsh
 * @since 2026-04-06
 */
@Service
@RequiredArgsConstructor
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements ICourseService {

}
