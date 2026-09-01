package com.xiaoyu.clinic.interceptor;

import com.xiaoyu.clinic.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component                                       // 把拦截器交给 Spring 容器管理
public class LoginInterceptor implements HandlerInterceptor {

    @Override                                    // 重写接口里的方法
    public boolean preHandle(HttpServletRequest req,   // 请求：能取 header、uri
                             HttpServletResponse resp, // 响应：能写状态码
                             Object handler) {
        // preHandle 在请求进入 Controller 之前执行
        // 返回 true = 放行；返回 false = 拦截


        if (req.getRequestURI().contains("/login")) {
            return true;
        }

        // 第 2 步：从请求头里取 token
        String token = req.getHeader("token");   // 前端必须把 token 放在 Header 的 token 字段

        if (token == null) {                     // 没带 token
            resp.setStatus(401);                 // 响应状态码设为 401（未授权）
            return false;                        // 拦截，不让进
        }

        // 第 3 步：解析 token（相当于验章）
        try {
            Claims claims = JwtUtils.parseToken(token);      // 解析成功 = token 合法
            req.setAttribute("userId", claims.getSubject()); // 把用户名塞进请求，后面的接口能取
            return true;                                     // 放行
        } catch (Exception e) {                  // 解析失败：token 被篡改 / 过期 / 乱写的
            resp.setStatus(401);                 // 401
            return false;                        // 拦截
        }
    }
}