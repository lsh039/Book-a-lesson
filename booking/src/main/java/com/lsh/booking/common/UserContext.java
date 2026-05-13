package com.lsh.booking.common;

/**
 * 线程级别的用户上下文，用于在整个请求的任意地方获取当前登录用户的 ID
 */
public class UserContext {
    // 使用 ThreadLocal 保存每个线程（每个请求）的 userId
    private static final ThreadLocal<Long> USER_THREAD_LOCAL = new ThreadLocal<>();

    // 存入 userId (在拦截器里调用)
    public static void setUserId(Long userId) {
        USER_THREAD_LOCAL.set(userId);
    }

    // 获取 userId (在 Service 层里随时调用，比如 myId = UserContext.getUserId())
    public static Long getUserId() {
        return USER_THREAD_LOCAL.get();
    }

    // 移除 userId (请求结束后必须清理，防止内存泄漏)
    public static void removeUser() {
        USER_THREAD_LOCAL.remove();
    }
}