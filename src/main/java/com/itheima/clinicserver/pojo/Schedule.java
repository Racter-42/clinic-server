package com.itheima.clinicserver.pojo;

public class Schedule {

    private Integer id;
    private Integer doctorId;
    private String  shiftDate;
    private Integer shiftType;
    private String createTime;
    private Integer version;

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

    public String getCreateTime() {
        return createTime;
    }
    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public Integer getVersion() {
        return version;
    }
    public void setVersion(Integer version) {
        this.version = version;
    }
}