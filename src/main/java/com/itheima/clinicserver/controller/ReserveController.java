package com.itheima.clinicserver.controller;

import com.itheima.clinicserver.pojo.Result;
import com.itheima.clinicserver.service.ReserveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Tag(name = "挂号预约", description = "挂号防重复提交 + 号源防超卖相关接口")
@RestController                                  // 接口类，返回 JSON
@RequestMapping("/reserve")                      // 统一前缀 /reserve
public class ReserveController {

    @Autowired
    private StringRedisTemplate redisTemplate;   // Redis 工具（只在"发 token"时用）

    @Autowired
    private ReserveService reserveService;       //  业务交给 Service（事务在 Service 里）

    // ========== 1. 拿防重 token（前端打开挂号页时调用）==========
    // 这个方法不涉及数据库写操作，不需要事务，留在 Controller 完全没问题
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

    // ========== 2. 提交挂号：防重复提交 + 号源防超卖 + 写预约记录 ==========
    @Operation(summary = "提交挂号（防重复提交 + 号源防超卖 + 写预约记录）")
    @PostMapping("")                             // 完整地址：POST /reserve
    //  注意：@Transactional 已经从这搬走了，放到 ReserveService.reserve() 上
    public Result reserve(@RequestParam String token,
                          @RequestParam Integer sourceId,        //  Long → Integer，与实体对齐
                          @RequestParam String patientName,      //  新增：患者姓名
                          @RequestParam String patientPhone) {   //  新增：患者手机号

        //  一行调用搞定所有业务逻辑
        // - 失败：Service 抛 BusinessException → GlobalExceptionHandler 转成 JSON（4004 / 4003 / 4001）
        // - 成功：Service 正常返回 → 下面这行执行，返回"预约成功"
        reserveService.reserve(sourceId, token, patientName, patientPhone);

        return Result.success("预约成功");
    }
}