package com.xiaoyu.clinic.controller;       // controller 包：对外接口

import com.xiaoyu.clinic.pojo.Result;      // 统一响应体
import com.xiaoyu.clinic.pojo.Schedule;
import com.xiaoyu.clinic.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "排班管理", description = "医生排班增删改查接口")
@RestController                              // 这是接口类，返回值直接当数据（不是页面）
@RequestMapping("/schedule")                 // 统一前缀：这个类里所有接口都以 /schedule 开头
public class ScheduleController {

    @Autowired                               // 注入 Service
    private ScheduleService service;

    // ========== 1. 新增排班 ==========
    @Operation(summary = "新增排班")
    @PostMapping("/add")                     // 完整地址：POST /schedule/add
    public Result add(@RequestBody Schedule s) {  // @RequestBody：把前端 JSON 转成 Schedule 对象
        service.insert(s);                   // 重复了会抛异常 → 全局处理器接管
        return Result.success();             // 没抛异常 = 成功，返回 {code:0, msg:"success"}
    }

    // ========== 2. 修改排班（带 version）==========
    @Operation(summary = "修改排班")
    @PutMapping("/update")                   // 完整地址：PUT /schedule/update
    public Result update(@RequestBody Schedule s) {
        service.update(s);                   // version 对不上会抛异常 → 全局处理器接管
        return Result.success();             // 成功
    }

    // ========== 3. 查询所有排班 ==========
    @Operation(summary = "查询所有排班")
    @GetMapping("/list")                     // 完整地址：GET /schedule/list
    public Result list() {
        return Result.success(service.listAll());  // 把列表塞进 data 字段返回
    }

    // ========== 4. 按科室 + 天数查询排班 ==========
    @Operation(summary = "按科室和天数查询排班")
    @GetMapping("/queryByDept")                     // 完整地址：GET /schedule/queryByDept
    public Result queryByDept(@RequestParam Integer deptId,        //  URL 里的 ?deptId=1
                              @RequestParam(defaultValue = "7") int days) {  //  ?days=7，不传默认 7
        return Result.success(service.queryByDept(deptId, days));  // 结果塞进 data 返回
    }

}