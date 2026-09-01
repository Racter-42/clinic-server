package com.xiaoyu.clinic.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtils {


    // 注意：必须 >= 32 个字符（算法要求密钥至少 256 位 = 32 字节）
    private static final String SECRET = "clinic-server-secret-key-2026-change-me-123456";

    // 有效期：24 小时，单位毫秒（1秒=1000毫秒），末尾 L 表示 long 类型
    private static final long EXPIRE = 24 * 60 * 60 * 1000L;

    // ========== 1. 生成 token（登录成功后调用） ==========
    public static String generateToken(String username, String role) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        // 上面这行：把字符串密钥转成签名算法认识的 SecretKey 对象（这里不需要深究，理解"转格式"即可）

        return Jwts.builder()                              // 开始组装 token
                .subject(username)                         // 放用户标识：谁登录的
                .claim("role", role)                       // 放自定义信息：他是什么角色
                .issuedAt(new Date())                      // 签发时间：现在
                .expiration(new Date(System.currentTimeMillis() + EXPIRE))  // 过期时间：24 小时后
                .signWith(key)                             // 盖章：用密钥签名，防止被人篡改
                .compact();                                // 打包成最终的字符串返回
    }

    // ========== 2. 解析 token（拦截器校验时调用） ==========
    public static Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()                               // 创建解析器
                .verifyWith(key)                           // 拿同一个密钥验章：签名对不对？
                .build()                                   // 解析器构建完成
                .parseSignedClaims(token)                  // 开始解析 token 字符串
                .getPayload();                             // 取出 payload（里面就是用户信息）
    }
}
