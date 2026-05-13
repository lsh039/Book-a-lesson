package com.lsh.booking.common;

import com.alibaba.fastjson.JSON;
import com.lsh.booking.utils.JwtUtils;
import com.lsh.booking.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String token = request.getHeader("Authorization");

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 1. 没token
        if (token == null || token.trim().isEmpty()) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"msg\":\"未登录或token无效\"}");
            return false;
        }

        try {
            // 2. 解析 token
            Long userId = JwtUtils.parseToken(token);
            String role = JwtUtils.getRole(token); // 👉 拿到角色

            // 3. 放入 request（后面 Controller 可以用）
            request.setAttribute("userId", userId);
            request.setAttribute("role", role); // 👉 顺手把角色也放进去，方便后续使用

            UserContext.setUserId(userId);

            // ================= 4. 无敌的越权拦截逻辑 =================
            // 如果请求的路径是以 /admin 开头，但角色不是 admin，直接踢掉！
            if (request.getRequestURI().startsWith("/admin") && !"admin".equals(role)) {
                response.setStatus(403); // 403 Forbidden 无权限
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(JSON.toJSONString(Result.error(403,"权限不足")));
                return false;
            }
            // ========================================================

        } catch (Exception e) {
            throw new BusinessException(401, "token无效");

        }

        return true;
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求无论是成功还是报错返回给前端了，最后都会走到这里。
        // 必须在这里清除 ThreadLocal，防止内存泄漏和串号！
        UserContext.removeUser();
    }
}
