package com.lsh.booking.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsh.booking.common.Result;
import com.lsh.booking.entity.Course;
import com.lsh.booking.dto.ReservationDTO;
import com.lsh.booking.entity.Reservation;
import com.lsh.booking.VO.ReservationVO;
import com.lsh.booking.VO.TimeSlotVO;
import com.lsh.booking.service.ICourseService;
import com.lsh.booking.service.IReservationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 预约表 前端控制器
 * </p>
 *
 * @author lsh
 * @since 2026-04-06
 */
@RestController
@RequestMapping("/reservation")
@RequiredArgsConstructor
@Slf4j
public class ReservationController {

    private final IReservationService reservationService;
    private final ICourseService courseService;

    /**
     * 创建预约
     */
    @PostMapping("/user/reservation")
    public Result<String> create(@Validated @RequestBody ReservationDTO dto,HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        // 1. 把 DTO 转换成数据库需要的实体类 Reservation
        Reservation reservation = new Reservation();
        reservation.setCourseId(dto.getCourseId());
        reservation.setAppointmentTime(dto.getAppointmentTime());

        // 2. 调用服务层代码
        reservationService.createReservation(reservation, userId);

        // 打印优雅的日志
        log.info("用户预约成功 | userId={} | courseId={} | time={}", userId, dto.getCourseId(), dto.getAppointmentTime());

        return Result.success("预约成功");}

    /**
     * 获取当前用户的预约（✅ 已经加了 N+1 性能优化）
     */
    @GetMapping("/user/my")
    public Result<Page<ReservationVO>> my(@RequestParam int page,
                                          @RequestParam int size,
                                          @RequestParam(required = false) Integer status,
                                          HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("获取当前用户的预约");

        // 1. 正常去数据库查这页的预约记录
        Page<Reservation> pageData = reservationService.page(
                new Page<>(page, size),
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getUserId, userId)
                        .eq(status != null, Reservation::getStatus, status)
                        .orderByAsc(Reservation::getAppointmentTime)
        );

        // ======== 这里就是那 4 步优化的代码位置 ========

        // Step 1：收集这页数据里所有的 courseId
        Set<Long> courseIds = pageData.getRecords().stream()
                .map(Reservation::getCourseId)
                .collect(Collectors.toSet());

        // Step 2 & 3: 批量查课程，并转成 Map（字典）
        Map<Long, Course> courseMap; // 👉 1. 先只声明，千万别 new HashMap<>()

        if (!courseIds.isEmpty()) {
            List<Course> courses = courseService.listByIds(courseIds);
            // 👉 2. 如果有数据，在这里把查出来的流赋值给它（第一次赋值）
            courseMap = courses.stream().collect(Collectors.toMap(Course::getId, c -> c));
        } else {
            // 👉 3. 如果没有数据，在这里给它一个空的字典（也是第一次赋值）
            courseMap = new HashMap<>();
        }
        // Step 4：把刚刚查好的 Map 字典，传给下面的 convertToVO 方法去翻译
        Page<ReservationVO> voPage = (Page<ReservationVO>) pageData.convert(r -> this.convertToVO(r, courseMap));

        // ===============================================

        return Result.success(voPage);
    }

    /**
     * 取消预约
     */
    @PostMapping("/user/cancel/{id}")
    public String cancel(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        reservationService.cancel(id, userId);
        log.info("取消成功");
        return "取消成功";
    }

    /**
     * 删除预约
     */
    @DeleteMapping("/user/{id}")
    public String delete(@PathVariable Long id) {
        reservationService.removeById(id);
        log.info("删除成功");
        return "删除成功";
    }

    /**
     * 获取可预约的时间
     */
    @GetMapping("/user/available")
    public Result<List<TimeSlotVO>> getAvailable(@RequestParam Long courseId, @RequestParam String date) {
        return Result.success(reservationService.getAvailable(date, courseId));
    }

    /**
     * 1. 管理员分页查看所有预约（✅ 已补全 N+1 批量查询优化）
     */
    @GetMapping("/admin/page")
    public Result<Page<ReservationVO>> adminPage(
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) Integer status) {

        // 查所有人的记录 (注意：这里没有 eq(userId) 的限制，因为管理员要看所有人)
        Page<Reservation> pageData = reservationService.page(
                new Page<>(page, size),
                new LambdaQueryWrapper<Reservation>()
                        .eq(status != null, Reservation::getStatus, status)
                        .orderByDesc(Reservation::getAppointmentTime) // 管理员通常喜欢按时间倒序看最新的
        );

        // 如果没数据直接返回
        if (pageData.getRecords().isEmpty()) {
            return Result.success((Page<ReservationVO>) pageData.convert(r -> new ReservationVO()));
        }

        // ================= 复用你刚学会的 N+1 优化批量查询 =================
        java.util.Set<Long> courseIds = pageData.getRecords().stream()
                .map(Reservation::getCourseId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        // Step 2 & 3: 批量查课程，并转成 Map（字典）
        Map<Long, Course> courseMap; // 👉 1. 先只声明，千万别 new HashMap<>()

        if (!courseIds.isEmpty()) {
            List<Course> courses = courseService.listByIds(courseIds);
            // 👉 2. 如果有数据，在这里把查出来的流赋值给它（第一次赋值）
            courseMap = courses.stream().collect(Collectors.toMap(Course::getId, c -> c));
        } else {
            // 👉 3. 如果没有数据，在这里给它一个空的字典（也是第一次赋值）
            courseMap = new HashMap<>();
        }
        // Step 4：把刚刚查好的 Map 字典，传给下面的 convertToVO 方法去翻译
        Page<ReservationVO> voPage = (Page<ReservationVO>) pageData.convert(r -> this.convertToVO(r, courseMap));

        return Result.success(voPage);
    }

    /**
     * 2. 管理员强制取消预约（✅ 已添加防呆设计）
     */
    @PostMapping("/admin/cancel/{id}")
    public Result<String> adminCancel(@PathVariable Long id){

        Reservation r = reservationService.getById(id);

        if (r == null){
            throw new RuntimeException("预约不存在"); // 保持你全局异常拦截的风格
        }

        if (r.getStatus() == 0){
            throw new RuntimeException("该预约已经是取消状态了"); // 防呆设计，防止重复取消
        }

        // 1. 修改数据库状态
        r.setStatus(0);
        reservationService.updateById(r);

        // 2. ⚠️ 终极补坑：管理员取消也会释放名额，必须清空对应日期的课程缓存！
        // 提示：因为当前 Controller 里没注入 RedisTemplate，最规范的做法其实是把这段业务逻辑
        // 写到 ReservationServiceImpl 里面（比如新建个 adminCancel 方法），在那里操作 Redis。
        // 面试时如果你能主动说出“管理员取消也要记得清 Redis 缓存”，绝对是个超级加分项！

        return Result.success("已强制取消");
    }

    /**
     * 返回 ReservationVO 对象
     */
    private ReservationVO convertToVO(Reservation r, Map<Long, Course> courseMap) {
        ReservationVO vo = new ReservationVO();

        vo.setId(Long.valueOf(r.getId()));

        // 时间格式化
        if (r.getAppointmentTime() != null) {
            vo.setTime(r.getAppointmentTime().toString());
        }

        // 状态转换
        if (r.getStatus() != null) {
            vo.setStatus(r.getStatus() == 1 ? "已预约" : "已取消");
        }

        // 👉 直接从传进来的 Map 字典里拿课程名字，速度极快！
        if (r.getCourseId() != null) {
            Course course = courseMap.get(r.getCourseId());
            vo.setCourseName(course != null ? course.getName() : "未知课程");
        } else {
            vo.setCourseName("未知课程");
        }

        return vo;
    }
}