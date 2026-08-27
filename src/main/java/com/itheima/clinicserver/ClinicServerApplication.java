package com.itheima.clinicserver;

import com.itheima.clinicserver.mapper.DoctorMapper;
import com.itheima.clinicserver.pojo.Doctor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class ClinicServerApplication /*implements CommandLineRunner*/ {

    // 注入你刚写好的Mapper接口
    @Autowired
    private DoctorMapper doctorMapper;

    public static void main(String[] args) {
        SpringApplication.run(ClinicServerApplication.class, args);
    }


    /*@Override
    public void run(String... args) throws Exception {
        // 测试动态 if：只传名字，看看会不会查出来
        List<Doctor> docs1 = doctorMapper.selectByCondition("王", null);
        System.out.println("按名字查出的数量：" + docs1.size());

        // 测试 foreach：传入两个 id 批量删除
        List<Integer> ids = new ArrayList<>();
        ids.add(1);
        ids.add(2); // 注意：前提是数据库里真的有 id 为 1 和 2 的数据
        doctorMapper.deleteBatch(ids);
        System.out.println("批量删除执行完毕！");
    }*/
}