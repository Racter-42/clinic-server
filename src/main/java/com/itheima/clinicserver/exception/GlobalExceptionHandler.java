package com.itheima.clinicserver.exception;   // exception 包：全局异常处理

import com.itheima.clinicserver.pojo.Result;  // 统一响应体
import org.springframework.web.bind.annotation.ExceptionHandler;      // 标记"处理哪类异常"
import org.springframework.web.bind.annotation.RestControllerAdvice;  // 全局异常处理器
import org.springframework.web.multipart.MaxUploadSizeExceededException;  // 文件大小超限异常

@RestControllerAdvice        //   @ControllerAdvice + @ResponseBody
public class GlobalExceptionHandler {   // 全局保安：所有 Controller 抛的异常都送到这

    /**
     * 上传文件超过大小限制（专用处理器，比下面的 Exception 兜底更具体，会优先命中）
     * 必须配合 application.properties 里的 spring.servlet.multipart.resolve-lazily=true，
     * 否则异常在进入 Controller 之前就被容器拦截，返回的是 Tomcat 的 HTML 错误页而不是 JSON
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        System.out.println("【全局异常处理】文件上传超限：" + e.getMessage());
        return Result.error("上传文件过大，单个文件不能超过 10MB");
    }

    @ExceptionHandler(Exception.class)   // ⭐ 捕获所有类型的异常（兜底）
    public Result handleException(Exception e) {   // 返回 Result，Spring 自动转 JSON
        String msg = e.getMessage();     // 取出异常里的错误描述

        // 只打印一行简短日志，不再刷满屏堆栈（printStackTrace 才是刷屏元凶）
        System.out.println("【全局异常处理】" + msg);

        // 情况 1：MySQL 唯一索引冲突（消息里必然带 Duplicate entry）
        if (msg != null && msg.contains("Duplicate entry")) {
            return Result.error("该医生该时段已排班，请勿重复添加");
        }

        // 情况 2：上传文件超过大小限制（兜底判断，配合下面的专用处理器）
        if (msg != null && msg.contains("Maximum upload size exceeded")) {
            return Result.error(msg + "，单个文件不能超过 10MB");
        }

        // 情况 3：其他所有异常，把原因原样返回
        return Result.error(msg);
    }
}
