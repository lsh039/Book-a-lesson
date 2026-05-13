package com.lsh.booking.service;

import com.lsh.booking.entity.Reservation;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsh.booking.VO.TimeSlotVO;

import java.util.List;

/**
 * <p>
 * 预约表 服务类
 * </p>
 *
 * @author lsh
 * @since 2026-04-06
 */
public interface IReservationService extends IService<Reservation> {

    /*
    * 创建预约
    * */
    void createReservation(Reservation reservation, Long userId);

    /*
    * 取消预约
    * */
    void cancel(Long id, Long userId);

    /*
    * 查询预约时间
    * */
    List<TimeSlotVO> getAvailable(String date, Long courseId);
}
