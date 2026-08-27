package com.itheima.clinicserver.controller;

import com.itheima.clinicserver.mapper.DoctorMapper;
import com.itheima.clinicserver.pojo.Doctor;
import com.itheima.clinicserver.service.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/doctor")
public class DoctorController {


    @Autowired
    private DoctorService service; // 注入刚才写的Mapper

    // 查询列表接口
    @GetMapping("/list")
    public List<Doctor> list() {
        return service.listAll();
    }

    // 新增接口
    @PostMapping("/add")
    public String add(@RequestBody Doctor d) {
        service.insert(d);
        return "成功";
    }

    @PutMapping("/update")
    public String update(@RequestBody Doctor d){service.update(d); return "成功";};

    @DeleteMapping("/delete/{id}")
    public String delete(@PathVariable Integer id){ service.delete(id); return  "成功";};
}
