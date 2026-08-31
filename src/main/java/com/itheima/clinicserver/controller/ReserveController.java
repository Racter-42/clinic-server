package com.itheima.clinicserver.controller;

import com.itheima.clinicserver.pojo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Tag(name = "挂号预约", description = "挂号防重复提交相关接口")
@RestController                                  // 接口类，返回 JSON
@RequestMapping("/reserve")                      // 统一前缀 /reserve
public class ReserveController {

    @Autowired
    private StringRedisTemplate redisTemplate;   // Redis 工具（存/删防重 token）

    // ========== 1. 拿防重 token（前端打开挂号页时调用）==========
    @Operation(summary = "获取防重提交 token")
    @GetMapping("/token")                        // 完整地址：GET /reserve/token
    public Result getReserveToken() {
        // ① 生成一个全球唯一的字符串（UUID），保证每个 token 不重复
        String token = UUID.randomUUID().toString();

        // ② SETNX 存进 Redis：reserve:token:xxx 不存在时才能存成功，10 分钟过期
        redisTemplate.opsForValue()
                .setIfAbsent("reserve:token:" + token, "1", 10, TimeUnit.MINUTES);

        // ③ 把 token 返回给前端（前端存起来，提交挂号时带上）
        return Result.success(token);
    }

    // ========== 2. 提交挂号：校验并删除 token（防重复提交）==========
    // ⚠️ 注意：方法路径写 ""（空字符串），不要写 "/reserve"！
    //   类上 @RequestMapping("/reserve") + @PostMapping("") = 实际路径 /reserve
    //   如果写成 @PostMapping("/reserve")，路径会变成 /reserve/reserve，Spring 找不到就报"No static resource"
    @Operation(summary = "提交挂号（防重复提交）")
    @PostMapping("")                            // 完整地址：POST /reserve
    public Result reserve(@RequestParam String token,
                                  @RequestParam Long sourceId) {
        // ④ 删 token：第一次删除成功(true)，第二次删除失败(false，key 已被删过)
        Boolean del = redisTemplate.delete("reserve:token:" + token);

        // ⑤ 删失败 = 重复提交，直接拒绝
        if (!Boolean.TRUE.equals(del)) {
            return Result.error("请勿重复提交");
        }

        // ⑥ 号源扣减逻辑（D13 实现：UPDATE source SET status=1 WHERE id=? AND status=0）
        // TODO: 号源扣减（D13 做，这里先占位）
        return Result.success("预约成功");
    }
}