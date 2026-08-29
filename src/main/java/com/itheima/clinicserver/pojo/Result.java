package com.itheima.clinicserver.pojo;   // pojo 包：统一返回结果

public class Result {                     // 统一响应体：所有接口都返回这个格式

    private Integer code;                 // 状态码：0 = 成功，1 = 失败
    private String  msg;                  // 提示信息：给前端/用户看的
    private Object  data;                 // 业务数据：查出来的列表、对象等

    // ===== 成功：不带数据 =====
    public static Result success() {       // static：直接 Result.success() 调用
        Result r = new Result();           // new 一个结果对象
        r.code = 0;                        // 0 代表成功
        r.msg  = "success";                // 默认提示语
        return r;                          // 返回组装好的对象
    }

    // ===== 成功：带数据 =====
    public static Result success(Object data) {   // 传入要返回的数据
        Result r = new Result();
        r.code = 0;
        r.msg  = "success";
        r.data = data;                     // 把数据塞进去
        return r;
    }

    // ===== 失败：只给提示 =====
    public static Result error(String msg) {      // 传入错误提示
        Result r = new Result();
        r.code = 1;                        // 1 代表失败
        r.msg  = msg;                      // 错误原因
        return r;
    }

    // ===== getter/setter：Spring 转 JSON 时要靠 getter =====
    public Integer getCode() {
        return code;
    }
    public void setCode(Integer code) {
        this.code = code;
    }
    public String getMsg() {
        return msg;
    }
    public void setMsg(String msg) {
        this.msg = msg;
    }
    public Object getData() {
        return data;
    }
    public void setData(Object data) {
        this.data = data;
    }
}
