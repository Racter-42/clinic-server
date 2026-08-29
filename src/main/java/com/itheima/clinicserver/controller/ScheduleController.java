package com.itheima.clinicserver.controller;       // controller 包：对外接口

import com.itheima.clinicserver.pojo.Result;      // 统一响应体
import com.itheima.clinicserver.pojo.Schedule;
import com.itheima.clinicserver.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController                              // 这是接口类，返回值直接当数据（不是页面）
@RequestMapping("/schedule")                 // 统一前缀：这个类里所有接口都以 /schedule 开头
public class ScheduleController {

    @Autowired                               // 注入 Service
    private ScheduleService service;

    // ========== 1. 新增排班 ==========
    @PostMapping("/add")                     // 完整地址：POST /schedule/add
    public Result add(@RequestBody Schedule s) {  // @RequestBody：把前端 JSON 转成 Schedule 对象
        service.insert(s);                   // 重复了会抛异常 → 全局处理器接管
        return Result.success();             // 没抛异常 = 成功，返回 {code:0, msg:"success"}
    }

    // ========== 2. 修改排班（带 version）==========
    @PutMapping("/update")                   // 完整地址：PUT /schedule/update
    public Result update(@RequestBody Schedule s) {
        service.update(s);                   // version 对不上会抛异常 → 全局处理器接管
        return Result.success();             // 成功
    }

    // ========== 3. 查询所有排班 ==========
    @GetMapping("/list")                     // 完整地址：GET /schedule/list
    public Result list() {
        return Result.success(service.listAll());  // 把列表塞进 data 字段返回
    }
}