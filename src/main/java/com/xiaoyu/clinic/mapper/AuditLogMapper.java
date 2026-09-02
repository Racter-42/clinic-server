package com.xiaoyu.clinic.mapper;

import com.xiaoyu.clinic.pojo.AuditLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper {

    // ========== 写一条操作审计 ==========
    // 注意：id、create_time 不写 —— 数据库自动填（自增 + DEFAULT CURRENT_TIMESTAMP）
    @Insert("insert into audit_log(user_id, operation, http_method, uri, ip, params, success, error_msg, cost_ms) "
            + "values(#{userId}, #{operation}, #{httpMethod}, #{uri}, #{ip}, #{params}, #{success}, #{errorMsg}, #{costMs})")
    int insert(AuditLog log);       // 返回值 int = 影响行数（成功插入 = 1）
}
