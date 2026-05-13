package com.lsh.booking.controller;


import com.lsh.booking.common.Result;
import com.lsh.booking.entity.User;
import com.lsh.booking.service.IUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author lsh
 * @since 2026-04-06
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final IUserService userService;

    /**
     *  注册
     * @param user
     * @return
     */
    @PostMapping("/register")
    public String register(@RequestBody User user){
        userService.register(user);
        log.info("注册成功");
        return "注册成功";
    }

    /**
     *
     * 登录
     * @param user
     * @return
     */
    @PostMapping("/login")
    public String login(@RequestBody User user){
        log.info("登录");
        return userService.login(user);
    }

    /**
     * 获取用户信息
     * @return
     */
    @GetMapping("/info")
    public Result<User> info(HttpServletRequest request){
        Long userId = (Long) request.getAttribute("userId");

        User user = userService.getById(userId);
        log.info("用户信息获取成功");
        return Result.success(user);
    }

}
