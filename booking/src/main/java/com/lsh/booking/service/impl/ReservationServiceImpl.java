package com.lsh.booking.service.impl;

import com.alibaba.fastjson.JSON;
import com.lsh.booking.entity.Reservation;
import com.lsh.booking.VO.TimeSlotVO;
import com.lsh.booking.exception.BusinessException;
import com.lsh.booking.mapper.ReservationMapper;
import com.lsh.booking.service.IReservationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 预约表 服务实现类
 * </p>
 *
 * @author lsh
 * @since 2026-04-06
 */
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl extends ServiceImpl<ReservationMapper, Reservation> implements IReservationService {

    private final StringRedisTemplate redisTemplate;

    private final ReservationMapper reservationMapper;
    /*
    * 创建预约
    * */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createReservation(Reservation reservation, Long userId) {

        String time = reservation.getAppointmentTime().toLocalTime().toString();

        String idemKey = "idem:reservation:"
                + userId + ":"
                + reservation.getCourseId() + ":"
                + time;

        // setIfAbsent = 只能成功一次
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(idemKey, "1", 5, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(first)) {
            throw new BusinessException("请勿重复提交");
        }

        //限流（同一时间只有几个人能退同时查）
        String limitKey = "limit:createReservation:" + userId;
        Long count = redisTemplate.opsForValue().increment(limitKey);
        if (count == 1) {
            // 如果是第一次访问，设置 1 秒的过期时间
            redisTemplate.expire(limitKey, 1, TimeUnit.SECONDS);
        }
        if (count > 5) {
            // 1秒内超过5次，直接踢掉，不往下走了
            throw new BusinessException("请求过于频繁，请稍后再试");
        }




        // 1、设置用户id
        reservation.setUserId(userId);

        String key = "lock:reservation:" + reservation.getCourseId() + ":" + reservation.getAppointmentTime();

        //获取锁
        String value = UUID.randomUUID().toString();

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, value, 10, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals( success)){
            throw new BusinessException("当前时间段太火爆，请稍后再试");
        }

        try {
            //2、判断是否重复预约
            Reservation exist = lambdaQuery()
                    .eq(Reservation::getUserId, userId)
                    .eq(Reservation::getAppointmentTime, reservation.getAppointmentTime())
                    .one();
            if (exist != null){
                throw new BusinessException("该时间段已预约");
            }

            Long currentCount = lambdaQuery().eq(Reservation::getAppointmentTime, reservation.getAppointmentTime())
                    .eq(Reservation::getCourseId, reservation.getCourseId())
                    .eq(Reservation::getStatus, 1)
                    .count();
            if (currentCount >= 10){
                throw new BusinessException("该课程已预约满");
            }


            //3、设置状态
            reservation.setStatus(1);

            //4、保存
            save(reservation);

            String cacheKey = "reservation:available:"
                    + reservation.getAppointmentTime().toLocalDate()
                    + ":" + reservation.getCourseId();

            redisTemplate.delete(cacheKey);
        } finally {
            String currentValue  = redisTemplate.opsForValue().get(key);
            if (value.equals(currentValue)){
                redisTemplate.delete(key);
            }

        }

    }

    /*
    * 取消预约
    * */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id, Long userId) {
        //获取Id
        Reservation reservation = getById(id);

        if (reservation == null){
            throw new BusinessException("预约不存在");
        }
        if (!reservation.getUserId().equals(userId)){
            throw new BusinessException("无权限取消预约");
        }

        if (reservation.getStatus() == 0){
            throw new BusinessException("预约已取消");
        }


        //设置返回权限
        reservation.setStatus(0);


        updateById(reservation);
        String cacheKey = "reservation:available:"
                + reservation.getAppointmentTime().toLocalDate()
                + ":" + reservation.getCourseId();
        redisTemplate.delete(cacheKey);


    }

    // ================= 新增这个私有方法 =================
    /**
     * 专门负责查数据库并组装数据的兜底方法
     */
    private List<TimeSlotVO> queryFromDB(String date, Long courseId) {
        // 1. 获取时间段
        List<String> timeSlots = Arrays.asList("09:00", "10:00", "11:00", "14:00", "15:00", "16:00");


        // 2. 查数据库获取预约时间
        List<Map<String,Object>> list = reservationMapper.countByTime(date, courseId);

        // 3. 统计数据
        HashMap<String, Integer> countMap = new HashMap<>();
        for (Map<String, Object> map : list) {
            String time = (String) map.get("time");
            Long num = (Long) map.get("num");
            countMap.put(time, num.intValue());
        }

        // 4. 组装最终的返回结果
        List<TimeSlotVO> result = new ArrayList<>();
        for (String time : timeSlots) {
            int count = countMap.getOrDefault(time, 0);

            TimeSlotVO vo = new TimeSlotVO();
            vo.setTime(time);
            vo.setAvailable(count < 10); // 小于10说明可以预约

            result.add(vo);
        }

        return result;
    }

    /**
     * 获取可预约时间段
     */
    @Override
    public List<TimeSlotVO> getAvailable(String date, Long courseId) {
        String key = "reservation:available:" + date + ":" + courseId;
        String lockKey = "lock:" + key;

        // 1. 查缓存
        String cache = redisTemplate.opsForValue().get(key);
        if (cache != null) {
            return JSON.parseArray(cache, TimeSlotVO.class);
        }

        // 2. 尝试加锁
        Boolean lock = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(lock)) {
            try {
                // double check
                cache = redisTemplate.opsForValue().get(key);
                if (cache != null) {
                    return JSON.parseArray(cache, TimeSlotVO.class);
                }

                // 查数据库
                List<TimeSlotVO> result = this.queryFromDB(date, courseId);

                // 写缓存
                redisTemplate.opsForValue().set(
                        key,
                        JSON.toJSONString(result),
                        5,
                        TimeUnit.MINUTES
                );

                return result;

            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            // 没拿到锁，稍等再查缓存
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            cache = redisTemplate.opsForValue().get(key);
            if (cache != null) {
                return JSON.parseArray(cache, TimeSlotVO.class);
            }
            // 超时
            for (int i = 0; i < 3; i++) {
                String cache1 = redisTemplate.opsForValue().get(key);
                if (cache1 != null) {
                    return JSON.parseArray(cache1, TimeSlotVO.class);
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return this.queryFromDB(date, courseId);
        }
    }

}
