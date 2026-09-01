package com.xiaoyu.clinic.exception;   // exception 包：全局异常处理

import com.xiaoyu.clinic.pojo.Result;              // 统一响应体
import org.slf4j.Logger;                                  // SLF4J 日志接口（Spring Boot 自带，无需加依赖）
import org.slf4j.LoggerFactory;                           // 获取 Logger 对象的工厂
import org.springframework.dao.DuplicateKeyException;     // 数据库唯一索引冲突异常（MyBatis 翻译出来的）
import org.springframework.web.bind.annotation.ExceptionHandler;      // 标记"处理哪类异常"
import org.springframework.web.bind.annotation.RestControllerAdvice;  // 全局异常处理器
import org.springframework.web.multipart.MaxUploadSizeExceededException;  // 文件大小超限异常

@RestControllerAdvice        //   @ControllerAdvice + @ResponseBody：横切所有 Controller + 返回 JSON
public class GlobalExceptionHandler {   // 全局保安：所有 Controller 抛的异常都送到这

    // 日志对象：log.error("xxx", e) 能把完整堆栈打出来，排查问题比 System.out.println 强
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 1. 业务异常（如"执业证号已存在"）——业务代码主动 throw new BusinessException(4001, "...")
     *    错误码和文案是业务代码抛的时候自己定的，这里原样取出来返回
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusiness(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 2. 数据库唯一索引冲突（如排班重复、执业证号重复）
     *    这是 MyBatis 在数据库违反唯一约束时抛的，异常对象里没有具体哪个字段冲突的信息，
     *    所以返回统一的 4001 + 通用提示（比原来用字符串匹配 "Duplicate entry" 更可靠）
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<?> handleDuplicateKey(DuplicateKeyException e) {
        log.error("数据重复（唯一索引冲突）", e);      // 完整堆栈进日志，方便查是哪张表冲突
        return Result.error(4001, "数据重复：可能是执业证号已存在或排班冲突");
    }

    /**
     * 3. 上传文件超过大小限制（专用处理器，比下面的 Exception 兜底更具体，会优先命中）
     *    必须配合 application.properties 里的 spring.servlet.multipart.resolve-lazily=true，
     *    否则异常在进入 Controller 之前就被容器拦截，返回的是 Tomcat 的 HTML 错误页而不是 JSON
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.error("文件上传超限", e);
        return Result.error(4002, "上传文件过大，单个文件不能超过 10MB");
    }

    /**
     * 4. 兜底（catch-all）：上面三段都没接住的异常全归这
     *    永远不把真实异常信息暴露给前端（防止泄露内部信息），统一返回 500 + 友好提示；
     *    真实异常通过 log.error("系统异常", e) 完整记录到日志，运维可查
     */
    @ExceptionHandler(Exception.class)   // ⭐ Exception 是所有异常（除 Error 体系外）的爸爸
    public Result<?> handleException(Exception e) {   // 返回 Result，Spring 自动转 JSON
        log.error("系统异常", e);        // 完整堆栈进日志文件（排查用），前端绝对不暴露
        return Result.error(500, "系统繁忙，请稍后再试");
    }
}
