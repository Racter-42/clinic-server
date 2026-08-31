package com.itheima.clinicserver.pojo;   // pojo 包：统一返回结果

/**
 * 统一响应体：所有接口都返回这个格式
 * 泛型 <T>：data 字段可以是任何类型（Doctor、List<Doctor>、null...），让"通吃"
 */
public class Result<T> {

    private Integer code;                 // 状态码：0 = 成功，非 0 = 失败（4001 业务错误 / 500 系统错误...）
    private String  message;              // 提示信息：给前端/用户看的
    private T       data;                 // 业务数据：查出来的列表、对象等

    // ===== 成功：不带数据 =====
    public static <T> Result<T> success() {       // static：直接 Result.success() 调用
        Result<T> r = new Result<>();             // new 一个结果对象
        r.code = 0;                               // 0 代表成功
        r.message = "success";                    // 默认提示语
        return r;                                 // 返回组装好的对象
    }

    // ===== 成功：带数据 =====
    public static <T> Result<T> success(T data) { // 传入要返回的数据
        Result<T> r = new Result<>();
        r.code = 0;
        r.message = "success";
        r.data = data;                            // 把数据塞进去
        return r;
    }

    // ===== 失败：只给提示（保留旧方法，兼容已有调用点）=====
    public static <T> Result<T> error(String message) {   // 传入错误提示
        Result<T> r = new Result<>();
        r.code = 1;                               // 1 代表失败
        r.message = message;                      // 错误原因
        return r;
    }

    // ===== 失败：带错误码（规范方式，业务异常 4001 / 系统异常 500...）=====
    public static <T> Result<T> error(Integer code, String message) {   // 传入错误码 + 错误提示
        Result<T> r = new Result<>();
        r.code = code;                            // 错误码（前端判断 code ≠ 0 就是失败）
        r.message = message;                      // 错误原因
        return r;
    }

    // ===== getter/setter：Spring 转 JSON 时要靠 getter =====
    public Integer getCode() {
        return code;
    }
    public void setCode(Integer code) {
        this.code = code;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public T getData() {
        return data;
    }
    public void setData(T data) {
        this.data = data;
    }
}
