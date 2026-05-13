package com.lsh.booking.service;

import com.lsh.booking.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author lsh
 * @since 2026-04-06
 */
public interface IUserService extends IService<User> {

    /*
    * 注册接口
    * */
    void register(User user);

    /*
    * 登录接口
    * */
    String login(User user);
}
