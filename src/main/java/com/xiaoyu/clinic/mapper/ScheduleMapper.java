package com.xiaoyu.clinic.mapper;   // mapper 包：专门写 SQL 的地方

import com.xiaoyu.clinic.pojo.Schedule;
import com.xiaoyu.clinic.pojo.ScheduleVO;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ScheduleMapper {

    // ========== 查询所有排班（方便测试时看结果）==========
    @Select("select * from schedule")      // @Select = 查询语句
    List<Schedule> listAll();              // 返回多个，用 List 装

    // ==========  查询未来 7 天的排班（定时任务用）==========
    @Select("select * from schedule "                            // 查排班表
            + "where shift_date >= curdate() "                   // 从今天开始
                    + "and shift_date < date_add(curdate(), interval 7 day)")  // 到今天+7天
    List<Schedule> findFuture7Days();

    // ==========  按科室 + 未来 N 天查询排班（连表拿医生名/科室名）==========
    @Select("select s.id, s.doctor_id as doctorId, "      // 排班ID、医生ID
            + "d.name as doctorName, "                    // 医生名字（doctor 表）
            + "s.shift_date as shiftDate, s.shift_type as shiftType, "  // 日期、班次
            + "d.dept_id as deptId, "                     // 科室ID（doctor 表）
            + "dept.name as deptName, "                   // 科室名字（clinic_dept 表）
            + "s.version "                                // 版本号
            + "from schedule s "                          // 主表：排班
            + "left join doctor d on s.doctor_id = d.id "               // 连医生表
            + "left join clinic_dept dept on d.dept_id = dept.id "      // 连科室表
            + "where d.dept_id = #{deptId} "              //  按科室过滤
            + "and s.shift_date >= curdate() "            // 从今天开始
            + "and s.shift_date < date_add(curdate(), interval #{days} day) "  // 未来 N 天
            + "order by s.shift_date, s.shift_type")      // 按日期排序
    List<ScheduleVO> queryByDept(@Param("deptId") Integer deptId,   //  方法参数
                                 @Param("days") int days);

    // ========== 新增排班（重复时数据库会抛异常）==========
    @Insert("insert into schedule(doctor_id, shift_date, shift_type, version) "  // 插入 4 个字段
            + "values(#{doctorId}, #{shiftDate}, #{shiftType}, 0)")
    // version 直接写 0：新记录的初始版本号就是 0
    void insert(Schedule schedule);        // 新增不需要返回值

    // ========== 修改排班（乐观锁核心！）==========
    @Update("update schedule "                          // 更新 schedule 表
            + "set shift_type = #{shiftType}, "         // 改班次
            + "version = version + 1 "                  // 版本号 +1（乐观锁的关键）
            + "where id = #{id} "                       // 改哪一条
            + "and version = #{version}")               //  必须匹配原来的版本号
    int update(Schedule schedule);         //  返回 int = 影响的行数（0 = 没改成）

}