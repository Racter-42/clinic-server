package com.itheima.clinicserver.exception;   // exception 包：和全局异常处理器放一起

/**
 * 自定义业务异常：业务规则不允许时主动抛（如"执业证号已存在""该医生当天已排班"）
 * extends RuntimeException 的原因：
 *   1. RuntimeException 是非受检异常，业务代码抛的时候不用在方法签名上写 throws
 *   2. Spring 事务回滚只对 RuntimeException（及其子类）生效，抛它才能让事务正确回滚
 */
public class BusinessException extends RuntimeException {

    private Integer code;      // 业务错误码（如 4001），由业务代码抛异常时传入
    private String message;    // 错误消息（如"执业证号已存在"），由业务代码抛异常时传入

    public BusinessException(Integer code, String message) {
        super(message);        // 把 message 传给父类 RuntimeException（父类自带 message 字段）
        this.code = code;      // 自己单独存 code（父类没有 code 字段，必须自己存）
        this.message = message; // 自己存 message（保证 getMessage() 返回的就是业务写的文案）
    }

    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
