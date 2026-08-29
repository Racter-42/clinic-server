package com.itheima.clinicserver.controller;

import com.itheima.clinicserver.mapper.SourceMapper;
import com.itheima.clinicserver.pojo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController                              // 接口类，返回 JSON
@RequestMapping("/source")                   // 统一前缀 /source
public class SourceController {

    @Autowired                               // 注入号源 Mapper
    private SourceMapper sourceMapper;       // 查询没有业务逻辑，直接用 Mapper 即可

    // ========== 查询未来 7 天可约号源（患者端用）==========
    @GetMapping("/list")                     // 完整地址：GET /source/list
    public Result list() {
        return Result.success(sourceMapper.listFuture7Days());   // 列表塞进 data 字段
    }
}