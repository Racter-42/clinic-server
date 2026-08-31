package com.itheima.clinicserver.controller;

import com.itheima.clinicserver.pojo.Doctor;
import com.itheima.clinicserver.pojo.Result;
import com.itheima.clinicserver.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "医生管理", description = "医生的增删改查接口")
@RestController
@RequestMapping("/doctor")
public class DoctorController {

    @Autowired
    private DoctorService service;

    @Operation(summary = "查询医生列表")
    @GetMapping("/list")
    public List<Doctor> list() {
        return service.listAll();
    }

    @Operation(summary = "新增医生")
    @PostMapping("/add")
    public String add(@RequestBody Doctor d) {
        service.insert(d);
        return "成功";
    }

    @Operation(summary = "更新医生")
    @PutMapping("/update")
    public String update(@RequestBody Doctor d) {
        service.update(d);
        return "成功";
    }

    @Operation(summary = "删除医生")
    @DeleteMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        service.delete(id);
        return "成功";
    }

    @Operation(summary = "统计科室在岗医生数")                    // ⭐ 新增
    @GetMapping("/countByDept")
    public Result countByDept() {
        return Result.success(service.countByDept());
    }
}