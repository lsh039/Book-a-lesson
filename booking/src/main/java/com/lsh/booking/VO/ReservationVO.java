package com.lsh.booking.VO;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReservationVO {

    private Long id;

    private String courseName; // 课程名（后面可以扩展）

    private String time; // 预约时间（字符串给前端）

    private String status; // 已预约 / 已取消
}