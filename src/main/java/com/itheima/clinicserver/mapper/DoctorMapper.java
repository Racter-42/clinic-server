package com.itheima.clinicserver.mapper;

import com.itheima.clinicserver.pojo.Doctor;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DoctorMapper {

    @Select("select * from doctor")
    List<Doctor> listAll();

    @Insert("insert into doctor(name, title, license_no) values(#{name}, #{title}, #{licenseNo})")
    void insert(Doctor doctor);

    @Update("update doctor set title = #{title} where id = #{id}")
    void update(Doctor doctor);

    @Delete("delete from doctor where id = #{id}")
    void delete(Integer id);

    @Select("select * from doctor limit #{offset}, #{pageSize}")
    List<Doctor> page(@Param("offset") int offset, @Param("pageSize") int pageSize);

    @Select("<script>" +
            "select * from doctor " +
            "where status = 0 " +
            "<if test='deptId != null'> and dept_id = #{deptId} </if>" +
            "<if test='title != null and title != \"\"'> and title = #{title} </if>" +
            "limit #{offset}, #{pageSize}" +
            "</script>")
    List<Doctor> pageByCondition(@Param("deptId") Integer deptId,
                                 @Param("title") String title,
                                 @Param("offset") int offset,
                                 @Param("pageSize") int pageSize);
}
