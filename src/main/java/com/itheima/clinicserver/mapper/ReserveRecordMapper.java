package com.itheima.clinicserver.mapper;

import com.itheima.clinicserver.pojo.ReserveRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReserveRecordMapper {

    // ========== 写一条预约记录 ==========
    // 注意：这里不写 id 和 create_time —— 数据库会自动填（自增 + DEFAULT CURRENT_TIMESTAMP）
    @Insert("insert into reserve_record(source_id, patient_name, patient_phone, status) "
            + "values(#{sourceId}, #{patientName}, #{patientPhone}, #{status})")
    int insert(ReserveRecord record);       // 返回值 int = 影响行数（成功插入 = 1）
}