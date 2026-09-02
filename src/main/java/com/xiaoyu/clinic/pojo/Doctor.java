package com.xiaoyu.clinic.pojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class Doctor {
    private Integer id;

    @NotBlank(message = "医生姓名不能为空")
    private String name;

    @NotBlank(message = "职称不能为空")
    private String title;

    // 执业证号：医疗规范要求 15 位数字；唯一性由数据库 uk_license 唯一索引保证
    @NotBlank(message = "执业证号不能为空")
    @Pattern(regexp = "^\\d{15}$", message = "执业证号应为 15 位数字")
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

    // toString：让审计日志能打出业务字段，而不是 Doctor@3bbc01aa
    @Override
    public String toString() {
        return "Doctor{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", title='" + title + '\'' +
                ", licenseNo='" + licenseNo + '\'' +
                '}';
    }
}
