package com.itheima.clinicserver.service;

import com.itheima.clinicserver.mapper.DoctorMapper;
import com.itheima.clinicserver.pojo.Doctor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DoctorService {

    @Autowired
    private DoctorMapper mapper;

    public List<Doctor> listAll(){ return mapper.listAll();}
    public void insert(Doctor d){ mapper.insert(d);}
    public void update(Doctor d){ mapper.update(d);}
    public void delete(Integer id){ mapper.delete(id);}

}
