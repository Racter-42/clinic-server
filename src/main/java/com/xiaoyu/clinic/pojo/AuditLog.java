package com.xiaoyu.clinic.pojo;   // pojo 包：放实体类

/**
 * 操作审计日志实体：一条记录 = 某人在某时对某个接口做了什么、成没成
 * 只存摘要信息（谁/什么接口/参数/成败/耗时），异常堆栈不往表里塞，太长
 */
public class AuditLog {

    private Long id;                 // 主键（数据库自增）
    private String userId;           // 操作人账号：拦截器塞到 request 里的 userId，没登录是 anonymous
    private String operation;        // 操作的接口：Controller.方法名（如 DoctorController.insert）
    private String httpMethod;       // 请求方式：GET/POST/PUT/DELETE
    private String uri;              // 请求路径（如 /doctor）
    private String ip;               // 来访 IP
    private String params;           // 请求参数（JSON 串，密码已打码、超长截断）
    private Integer success;         // 1 成功 / 0 失败
    private String errorMsg;         // 失败原因（成功时为 null）
    private Integer costMs;          // 接口耗时（毫秒）
    private String createTime;       // 操作时间（插入时不填，数据库自动生成）

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String getOperation() {
        return operation;
    }
    public void setOperation(String operation) {
        this.operation = operation;
    }
    public String getHttpMethod() {
        return httpMethod;
    }
    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }
    public String getUri() {
        return uri;
    }
    public void setUri(String uri) {
        this.uri = uri;
    }
    public String getIp() {
        return ip;
    }
    public void setIp(String ip) {
        this.ip = ip;
    }
    public String getParams() {
        return params;
    }
    public void setParams(String params) {
        this.params = params;
    }
    public Integer getSuccess() {
        return success;
    }
    public void setSuccess(Integer success) {
        this.success = success;
    }
    public String getErrorMsg() {
        return errorMsg;
    }
    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
    public Integer getCostMs() {
        return costMs;
    }
    public void setCostMs(Integer costMs) {
        this.costMs = costMs;
    }
    public String getCreateTime() {
        return createTime;
    }
    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
}
