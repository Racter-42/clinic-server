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

    // 动态条件查询
    /*@Select("<script>" +
            "select * from doctor where 1=1 " +
            "<if test='name != null and name != \"\"'> and name like concat('%', #{name}, '%') </if>" +
            "<if test='title != null and title != \"\"'> and title = #{title} </if>" +
            "</script>")
    List<Doctor> selectByCondition(@Param("name") String name, @Param("title") String title);

    // 批量删除
    @Delete("<script>" +
            "delete from doctor where id in " +
            "<foreach collection='list' item='id' open='(' separator=',' close=')'>#{id} </foreach>" +
            "</script>")
    void deleteBatch(List<Integer> ids);*/
}
