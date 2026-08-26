package com.itheima.clinicserver.service;

import org.springframework.stereotype.Service;

@Service
public class DoctorService {

    // 先模拟数据（等D10这里会注入 Mapper，然后查数据库）
    public String getDoctorInfo() {
        // 假装这里的逻辑是：只查状态为1的医生，并按工龄排序（业务规则）
        // 现在先返回一句话，证明我被调到了
        return "我是Service层：我已经处理了业务逻辑，这是返回的医生列表（模拟数据）";
    }

}
