package com.itheima.clinicserver.mapper;

import com.itheima.clinicserver.pojo.Doctor;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

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

    // ========== 科室在岗医生数统计 ==========
    @Select("select d.dept_id as deptId, "                    // 科室ID
            + "dept.name as deptName, "                       // 科室名称（连表查出来的）
                   + "count(*) as doctorCount "                      // 统计人数
                   + "from doctor d "                                // 主表：医生表
                   + "left join clinic_dept dept on d.dept_id = dept.id "  // 左连接科室表，拿科室名
                   + "where d.status = 0 "                           //  只统计在岗（0=在岗）
                   + "group by d.dept_id, dept.name "                //  按科室分组
                   + "order by d.dept_id")                           // 按科室ID排序
    List<Map<String, Object>> countByDept();                  // 用 Map 装结果

}
