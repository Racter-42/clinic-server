package com.itheima.clinicserver.controller;

import com.itheima.clinicserver.pojo.Doctor;
import com.itheima.clinicserver.service.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.print.Doc;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/doctor")
public class DoctorController {


    private static List<Doctor> doctorList = new ArrayList<>();

    static {
        doctorList.add(new Doctor(1,"王五一","主治医师"));
        doctorList.add(new Doctor(2,"李六六","副主任医师"));
    }

    @GetMapping("/list")
    public List<Doctor> list(){
        return doctorList;
    }

    @PostMapping("/add")
    public String add(@RequestBody Doctor doctor){

        doctorList.add(doctor);
        return "新增成功";
    }

    @DeleteMapping("/delete/{id}")
    public String delete(@PathVariable Integer id){
        doctorList.removeIf(doctor -> doctor.getId().equals(id));
        return "删除成功";
    }
}
