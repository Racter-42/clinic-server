package com.itheima.clinicserver.mapper;   // mapper 包

import com.itheima.clinicserver.pojo.Source;
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
    @Select("select * from source "
            + "where shift_date >= curdate() "                   // curdate() = 今天
            + "and shift_date < date_add(curdate(), interval 7 day)")  // 今天+7天之前
    List<Source> listFuture7Days();         // 返回未来 7 天的号源列表
}