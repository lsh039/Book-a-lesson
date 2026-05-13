package com.lsh.booking.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;

public class JwtUtils {

    private static final String SECRET = "lsh_secret_key";

    // 👉 改造：生成 token 时，不仅要 userId，还要把 role 传进来
    public static String generateToken(Long userId, String role) {
        return Jwts.builder()
                .claim("role", role) // 往 token 里塞入自定义字段 role
                .setSubject(userId.toString())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
    }

    // 保持不变：解析获取 userId（不影响你之前的代码）
    public static Long parseToken(String token) {
        Claims claims = getClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    // 👉 新增：解析获取 role 权限角色
    public static String getRole(String token) {
        Claims claims = getClaims(token);
        // 取出我们刚才在 generateToken 里存的 "role"
        return claims.get("role", String.class);
    }

    // 提取公共方法，避免代码重复
    private static Claims getClaims(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET)
                .parseClaimsJws(token)
                .getBody();
    }
}
