package com.itheima.clinicserver.service;   // service 包：写业务逻辑

import com.itheima.clinicserver.mapper.ScheduleMapper;
import com.itheima.clinicserver.pojo.Schedule;
import com.itheima.clinicserver.pojo.ScheduleVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service                                    // 告诉 Spring：这是业务类，帮我管理
public class ScheduleService {

    @Autowired                              // 把 MyBatis 生成的 Mapper 实现拿过来用
    private ScheduleMapper mapper;

    // ========== 1. 新增排班（唯一索引防重）==========
    public void insert(Schedule s) {
        // 重复插入时，数据库的唯一索引会抛异常
        // 这里故意不 catch —— 让异常往上抛，交给 GlobalExceptionHandler 统一转成友好提示
        // 这样代码干净，也不会在控制台刷满异常堆栈
        mapper.insert(s);
    }

    // ========== 2. 修改排班（乐观锁防覆盖）==========
    public void update(Schedule s) {
        int rows = mapper.update(s);         // 拿到影响行数：1 = 成功，0 = 失败
        if (rows == 0) {                     // 0 行 = version 对不上 = 被别人改过了
            throw new RuntimeException("数据已被其他人修改，请刷新后重试");
        }
        // rows == 1：更新成功，什么都不用做
    }

    // ========== 3. 查询所有排班 ==========
    public List<Schedule> listAll() {
        return mapper.listAll();             // 直接调 Mapper，没有额外逻辑
    }

    // ========== 4. 按科室 + 天数查询排班 ==========
    public List<ScheduleVO> queryByDept(Integer deptId, int days) {  // 参数从 Controller 传进来
        return mapper.queryByDept(deptId, days);   // 纯查询，直接调 Mapper
    }

}