package com.xiaoyu.clinic.mapper;

import com.xiaoyu.clinic.pojo.ReserveRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ReserveRecordMapper {

    // ========== 写一条预约记录 ==========
    // 注意：这里不写 id 和 create_time —— 数据库会自动填（自增 + DEFAULT CURRENT_TIMESTAMP）
    @Insert("insert into reserve_record(source_id, patient_name, patient_phone, status) "
            + "values(#{sourceId}, #{patientName}, #{patientPhone}, #{status})")
    int insert(ReserveRecord record);       // 返回值 int = 影响行数（成功插入 = 1）

    // ========== 按手机号查这位患者约过哪些号（导诊台高频操作） ==========
    // 为什么 JOIN 两张表？
    //   reserve_record 只存 source_id，导诊台要看到"约了哪个医生、哪天、哪个时段"，
    //   得顺着 source_id 去 source 表拿日期时段、再顺着 doctor_id 去 doctor 表拿姓名。
    //   一条 SQL 连表带出来，前端不用再发两次请求（SourceMapper / ScheduleMapper 都是这个套路）。
    // where patient_phone = ? 落在 idx_patient_phone 索引上 → type=ref 单行命中，不会全表扫描
    @Select("select r.id, r.source_id as sourceId, r.patient_name as patientName, "
            + "r.patient_phone as patientPhone, r.status, r.create_time as createTime, "
            + "s.shift_date as shiftDate, s.time_slot as timeSlot, d.name as doctorName "
            + "from reserve_record r "
            + "left join source s on r.source_id = s.id "
            + "left join doctor d on s.doctor_id = d.id "
            + "where r.patient_phone = #{phone} "
            + "order by r.create_time desc")
    List<ReserveRecord> findByPatientPhone(@Param("phone") String phone);
}