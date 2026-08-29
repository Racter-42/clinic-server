package com.itheima.clinicserver.pojo;

public class Source {

    private Integer id;
    private Integer doctorId;
    private String  shiftDate;
    private String  timeSlot;
    private Integer status;
    private String  createTime;

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

    public String getTimeSlot() {
        return timeSlot;
    }
    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
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