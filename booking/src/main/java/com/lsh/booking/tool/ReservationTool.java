package com.lsh.booking.tool;

import com.lsh.booking.common.UserContext;
import com.lsh.booking.entity.Course;
import com.lsh.booking.entity.Reservation;
import com.lsh.booking.service.ICourseService;
import com.lsh.booking.service.IReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReservationTool {

    private final ICourseService courseService;
    private final IReservationService reservationService;

    // 1：让 AI 有能力自己去查课程表，而不是每次都把整个表塞给它
    @Tool(description = "查询当前系统内所有可预约的课程列表")
    public List<Course> queryAvailableCourses() {
        return courseService.list();
    }

    //  2：核心动作！让 AI 帮你解析参数并触发预约
    @Tool(description = "生成预约单，帮助用户预约具体的课程")
    public String createReservation(
            @ToolParam(description = "课程名称，例如：数学、瑜伽") String courseName,
            @ToolParam(description = "预约时间，格式必须为 yyyy-MM-dd HH:mm:ss。如果用户没有说具体时间，请默认传明天上午10点") String timeStr
    ) {
        // 1. 拦截未登录
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return "预约失败：请告诉用户，需要先登录系统才能预约。";
        }

        // 2. 匹配课程
        List<Course> courses = courseService.list();
        Course targetCourse = courses.stream()
                .filter(c -> c.getName().contains(courseName))
                .findFirst()
                .orElse(null);

        if (targetCourse == null) {
            return "预约失败：未找到该课程，请温柔地告诉用户换一门课。";
        }

        // 3. 执行安全的预约逻辑
        try {
            Reservation reservation = new Reservation();
            reservation.setCourseId(targetCourse.getId());

            // AI非常聪明，它会把用户说的话转化成 yyyy-MM-dd HH:mm:ss 传进来
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime appointmentTime = LocalDateTime.parse(timeStr, formatter);
            reservation.setAppointmentTime(appointmentTime);

            // 调用你写了 Redis 分布式锁的底层方法！
            reservationService.createReservation(reservation, userId);

            return "操作成功！已经为用户预约了：" + targetCourse.getName() + "，时间是：" + timeStr;
        } catch (Exception e) {
            // 把底层抛出的超卖、限流等错误告诉 AI，让 AI 去转达给用户
            return "预约失败：" + e.getMessage();
        }
    }
}