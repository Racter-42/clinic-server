package com.xiaoyu.clinic.controller;

import com.xiaoyu.clinic.pojo.LoginDTO;
import com.xiaoyu.clinic.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*; // 引入 Spring MVC 的注解（* = 全部）


@Tag(name = "登录", description = "登录相关接口")
@RestController                                  // 告诉 Spring：这是"接口类"，返回值直接当数据返回
public class LoginController {


    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public String login(@RequestBody LoginDTO dto) {
        // @RequestBody：把前端 JSON 自动转成 LoginDTO 对象
        // 前端传 {"username":"admin","password":"123456"}
        // → dto.username="admin"，dto.password="123456"

        if ("admin".equals(dto.getUsername())    // 判断用户名是不是 admin（常量放前面防空指针）
                && "123456".equals(dto.getPassword())) {
            // 两个都对 → 生成 token 返回
            String token = JwtUtils.generateToken("admin", "院长"); // 参数1:用户名 参数2:角色
            return token;                        // 把 token 字符串返回给前端
        }
        // 有一个不对 → 抛异常
        throw new RuntimeException("用户名或密码错误");
    }
}