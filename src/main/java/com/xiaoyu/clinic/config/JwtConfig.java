package com.xiaoyu.clinic.config;

import com.xiaoyu.clinic.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

// JWT 密钥注入：JwtUtils 是静态工具类，不能直接用 @Value 注入字段，
// 由这个配置类在应用启动时读取 jwt.secret，再调用 JwtUtils.setSecret() 桥接进去
@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")
    public void setJwtSecret(String secret) {
        JwtUtils.setSecret(secret);
    }
}
