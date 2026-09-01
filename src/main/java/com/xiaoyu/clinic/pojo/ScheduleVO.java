package com.xiaoyu.clinic.pojo;   // pojo 包

public class ScheduleVO {                 // VO：专门装"查询出来的结果"

    private Integer id;                   // 排班ID（来自 schedule.id）
    private Integer doctorId;             // 医生ID
    private String  doctorName;           // ⭐ 医生名字（连表从 doctor 表拿的）
    private String  shiftDate;            // 排班日期
    private Integer shiftType;            // 班次：1上午/2下午/3晚班
    private Integer deptId;               // 科室ID（来自 doctor.dept_id）
    private String  deptName;             // ⭐ 科室名字（连表从 clinic_dept 表拿的）
    private Integer version;              // 乐观锁版本号

    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getDoctorId() {
        return doctorId;
    }
    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }
    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getShiftDate() {
        return shiftDate;
    }
    public void setShiftDate(String shiftDate) {
        this.shiftDate = shiftDate;
    }

    public Integer getShiftType() {
        return shiftType;
    }
    public void setShiftType(Integer shiftType) {
        this.shiftType = shiftType;
    }

    public Integer getDeptId() {
        return deptId;
    }
    public void setDeptId(Integer deptId) {
        this.deptId = deptId;
    }

    public String getDeptName() {
        return deptName;
    }
    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public Integer getVersion() {
        return version;
    }
    public void setVersion(Integer version) {
        this.version = version;
    }
}