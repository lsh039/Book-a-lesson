package com.lsh.booking.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsh.booking.common.Result;
import com.lsh.booking.entity.Course;
import com.lsh.booking.service.ICourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 课程/服务表 前端控制器
 * </p>
 *
 * @author lsh
 * @since 2026-04-06
 */
@RestController
@RequestMapping("/course")
@RequiredArgsConstructor
@Slf4j
public class CourseController {

    private final ICourseService courseService;

    /**
     * 添加课程
     * @param course
     * @return
     */
    @PostMapping("/admin/add")
    public Result<String> add(@RequestBody Course course){
        courseService.save(course);
        log.info("添加课程：{}", course);
        return Result.success("添加成功");
    }

    /**
     * 删除课程
     * @param id
     * @return
     */
    @DeleteMapping("/admin/{id}")
    public Result<String> delete(@PathVariable Long id){
        courseService.removeById(id);
        log.info("删除课程：{}", id);
        return Result.success("删除成功");
    }

    /**
     * 分页查询课程
     * @param page
     * @param size
     * @return
     */
    @GetMapping("/admin/page")
    public Result<Page<Course>> page(@RequestParam int page,
                                     @RequestParam int size){
        return Result.success(
                courseService.page(new Page<>(page, size))
        );
    }

    /**
     * 获取课程列表
     * @return
     */
    @GetMapping("/list")
    public Result<List<Course>> list(){
        return Result.success(
                courseService.lambdaQuery()
                        .eq(Course::getStatus, 1)
                        .list()
        );
    }
    /**
     * 修改课程
     * @param
     * @return
     */
    @PutMapping("/admin/update")
    public Result<String> update(@RequestBody Course course){
        courseService.updateById(course);
        return Result.success("修改成功");
    }


}
