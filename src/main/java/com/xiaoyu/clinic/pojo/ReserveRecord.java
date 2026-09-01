package com.xiaoyu.clinic.pojo;   // pojo 包：和 Source、Doctor 放一起

/**
 * 预约记录实体类：对应数据库 reserve_record 表的一行
 * 每个字段的类型和数据库列一一对应（INT → Integer，VARCHAR → String，TINYINT → Integer）
 */
public class ReserveRecord {

    private Integer id;             // 主键（插入时不用管，MySQL 自动自增）
    private Integer sourceId;       // 号源 ID（对应 source.id，用 Integer 与 Source 实体对齐）
    private String  patientName;    // 患者姓名
    private String  patientPhone;   // 患者手机号
    private Integer status;         // 1已预约 2已取消
    private String  createTime;     // 预约时间（插入时不用管，MySQL 自动填当前时间）


    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSourceId() {
        return sourceId;
    }
    public void setSourceId(Integer sourceId) {
        this.sourceId = sourceId;
    }

    public String getPatientName() {
        return patientName;
    }
    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientPhone() {
        return patientPhone;
    }
    public void setPatientPhone(String patientPhone) {
        this.patientPhone = patientPhone;
    }

    public Integer getStatus() {
        return status;
    }
    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getCreateTime() {
        return createTime;
    }
    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
}