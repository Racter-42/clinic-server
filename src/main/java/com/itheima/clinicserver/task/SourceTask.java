package com.itheima.clinicserver.task;      // task 包：放定时任务

import com.itheima.clinicserver.mapper.ScheduleMapper;
import com.itheima.clinicserver.mapper.SourceMapper;
import com.itheima.clinicserver.pojo.Schedule;
import com.itheima.clinicserver.pojo.Source;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Component                                   // 必须加：让 Spring 扫描到这个类
public class SourceTask {

    @Autowired                               // 注入排班 Mapper：查未来 7 天排班
    private ScheduleMapper scheduleMapper;

    @Autowired                               // 注入号源 Mapper：插入号源
    private SourceMapper sourceMapper;

    // 核心注解：cron 表达式决定"什么时候执行"
    // "0 * * * * ?"   = 每分钟的第 0 秒执行（测试用，方便立刻看到效果）
    // "0 0 2 * * ?"   = 每天凌晨 02:00:00 执行（正式环境用这个）
    @Scheduled(cron = "0 0 2 * * ?")
    public void generateSource() {           // 方法名随便起，重点是上面的注解

        // 第 1 步：查出未来 7 天的所有排班
        List<Schedule> list = scheduleMapper.findFuture7Days();

        // 第 2 步：遍历每条排班，把它"翻译"成号源
        for (Schedule s : list) {            // 一个排班 → 多个号源

            // 第 3 步：根据班次类型，决定切成哪些时段
            String[] slots = getTimeSlots(s.getShiftType());   // 比如上午返回 3 个时段

            // 第 4 步：逐个时段插入号源表
            for (String slot : slots) {
                try {
                    Source source = new Source();              // new 一个号源对象
                    source.setDoctorId(s.getDoctorId());       // 医生ID：从排班复制
                    source.setShiftDate(s.getShiftDate());     // 日期：从排班复制
                    source.setTimeSlot(slot);                  // 时段：切出来的那个
                    sourceMapper.insert(source);               // 插入数据库
                } catch (Exception e) {
                    // 重复的号源直接忽略（唯一索引已经拦住了，我们不用管）
                    // 这里必须 try-catch！
                }
            }
        }

        System.out.println("【定时任务】号源生成完毕");
    }

    // ========== 辅助方法：根据班次返回时段数组 ==========
    private String[] getTimeSlots(Integer shiftType) {
        if (shiftType == 1) {                // 1 = 上午
            return new String[]{"08:00-09:00", "09:00-10:00", "10:00-11:00"};
        } else if (shiftType == 2) {         // 2 = 下午
            return new String[]{"14:00-15:00", "15:00-16:00", "16:00-17:00"};
        } else {                             // 3 = 晚班
            return new String[]{"18:00-19:00", "19:00-20:00"};
        }
    }
}