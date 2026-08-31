package com.itheima.clinicserver.controller;

import com.itheima.clinicserver.pojo.Result;
import com.itheima.clinicserver.service.SourceService;   // 改为注入 Service（原来直接注入 Mapper）
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "号源管理", description = "号源查询接口")
@RestController
@RequestMapping("/source")
public class SourceController {

    @Autowired
    private SourceService sourceService;        // 注入 Service（缓存逻辑在 Service 里）

    // ========== 查询未来 7 天可约号源（患者端用，带缓存）==========
    @Operation(summary = "查询未来7天可约号源")
    @GetMapping("/list")
    public Result list() {
        return Result.success(sourceService.listFuture7Days());   // 调 Service（内部有缓存三防）
    }
}