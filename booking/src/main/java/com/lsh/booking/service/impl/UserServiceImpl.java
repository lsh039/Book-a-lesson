package com.lsh.booking.service.impl;

import com.lsh.booking.utils.JwtUtils;
import com.lsh.booking.entity.User;
import com.lsh.booking.exception.BusinessException;
import com.lsh.booking.mapper.UserMapper;
import com.lsh.booking.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author lsh
 * @since 2026-04-06
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    public void register(User user) {
        //1、简单校验
        if (user.getUsername() == null || user.getPassword() ==null){
            throw new BusinessException("用户名或密码不能为空");
        }
        //2、判断是否存在
        User exist = lambdaQuery()
                .eq(User::getUsername, user.getUsername())
                .one();
        if (exist != null){
            throw new BusinessException("用户已存在");
        }
        //3、保存
        save(user);

    }

    @Override
    public String login(User user) {
        //判断是否相同
        User username = lambdaQuery()
                .eq(User::getUsername, user.getUsername())
                .one();

        if (username == null){
            throw new BusinessException("用户不存在");
        }
        if (!username.getPassword().equals(user.getPassword())){
            throw new BusinessException("密码错误");
        }


        // 动态获取当前登录用户的真实角色
        String role = username.getRole();

        // 如果数据库里这个字段碰巧是空的，给他一个兜底的普通用户权限，防报错
        if (role == null || role.trim().isEmpty()) {
            role = "user";
        }
        //生成token
        String token = JwtUtils.generateToken(username.getId(),role);

        return token;

    }
}
