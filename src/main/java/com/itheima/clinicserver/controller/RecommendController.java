package com.itheima.clinicserver.controller;

import com.itheima.clinicserver.pojo.Result;
import com.itheima.clinicserver.service.DeepseekService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "智能导诊", description = "基于 Deepseek 的症状导诊接口")
@RestController
@RequestMapping("/api")
public class RecommendController {

    @Autowired
    private DeepseekService deepseekService;

    @Operation(summary = "根据症状推荐科室")
    @PostMapping("/recommend")
    public Result<String> recommend(@RequestBody Map<String, String> params) {
        String symptom = params.get("symptom");

        // 参数校验：空症状不去调 API（省 token，也避免 AI 瞎编）
        if (symptom == null || symptom.isBlank()) {
            return Result.error(4001, "症状描述不能为空");
        }

        // 注意：这里永远是 Result.success —— 即使 API 挂了也是 success，
        // 因为降级文案对前端来说是"正常业务结果"，不是错误
        return Result.success(deepseekService.recommend(symptom));
    }
}

