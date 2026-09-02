package com.xiaoyu.clinic.mapper;   // mapper 包

import com.xiaoyu.clinic.pojo.Source;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper                                     // 告诉 MyBatis：这是 Mapper 接口
public interface SourceMapper {

    // ========== 1. 插入一条号源（重复时数据库会抛异常）==========
    @Insert("insert into source(doctor_id, shift_date, time_slot, status) "
            + "values(#{doctorId}, #{shiftDate}, #{timeSlot}, 0)")
    // status 直接写 0：新生成的号源都是"可约"状态
    void insert(Source source);

    // ========== 2. 查询未来 7 天可约号源 ==========
    // LEFT JOIN doctor：号源表只存 doctor_id，页面要显示医生姓名，
    // 查的时候顺手把 doctor 表的 name 带出来，前端就不用自己再查一次医生接口了
    @Select("select s.*, d.name as doctorName from source s "
            + "left join doctor d on s.doctor_id = d.id "
            + "where s.shift_date >= curdate() "                   // curdate() = 今天
            + "and s.shift_date < date_add(curdate(), interval 7 day)")  // 今天+7天之前
    List<Source> listFuture7Days();         // 返回未来 7 天的号源列表（带医生姓名）

    // ========== 3. 号源扣减（乐观锁：只更新"还空着"的号源）==========
    // 关键：把"判断"和"修改"压进一条 SQL，靠数据库的行锁保证原子性
    @Update("update source set status = 1 "
            + "where id = #{id} and status = 0")
    int reserve(@Param("id") Integer id);   // 返回值 int = SQL 影响行数（1=抢到，0=没抢到）

}