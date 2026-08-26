package com.itheima.clinicserver.pojo;

public class Doctor {
    private Integer id;
    private String name;
    private String department;
    private String title;

    public Doctor() {
    }

    public Doctor(Integer id, String name, String title) {
        this.id = id;
        this.name = name;
        this.title = title;

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

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
