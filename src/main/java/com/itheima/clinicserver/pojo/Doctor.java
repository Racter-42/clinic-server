package com.itheima.clinicserver.pojo;

public class Doctor {
    private Integer id;
    private String name;
    private String title;
    private String licenseNo;


    public Doctor() {
    }

    public Doctor(Integer id, String name, String title,String licenseNo) {
        this.id = id;
        this.name = name;
        this.title = title;
        this.licenseNo = licenseNo;

    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLicenseNo() {
        return licenseNo;
    }

    public void setLicenseNo(String licenseNo) {
        this.licenseNo = licenseNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
