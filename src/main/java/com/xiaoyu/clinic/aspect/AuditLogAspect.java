package com.xiaoyu.clinic.aspect;

import com.alibaba.fastjson2.JSON;
import com.xiaoyu.clinic.mapper.AuditLogMapper;
import com.xiaoyu.clinic.pojo.AuditLog;
import com.xiaoyu.clinic.pojo.LoginDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Aspect
@Component
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);

    // 光打日志不行：日志文件会滚动、服务重启后旧记录就翻不到了，
    // 医疗合规要求"谁动了什么"能随时查，所以每条操作落一张 audit_log 表
    @Autowired
    private AuditLogMapper auditLogMapper;

    @Around("execution(* com.xiaoyu.clinic.controller..*.*(..))")
    public Object audit(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();

        // 从请求上下文拿 IP 和当前登录人
        // （登录接口本身没登录，userId 为空，统一记 anonymous）
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        HttpServletRequest req = ((ServletRequestAttributes) attrs).getRequest();
        String ip = req.getRemoteAddr();
        String user = (String) req.getAttribute("userId");
        if (user == null) {
            user = "anonymous";
        }

        // 组装审计记录：先填公共字段，成败和耗时得等接口跑完才知道
        AuditLog audit = new AuditLog();
        audit.setUserId(user);
        audit.setOperation(pjp.getSignature().getDeclaringType().getSimpleName()
                + "." + pjp.getSignature().getName());
        audit.setHttpMethod(req.getMethod());
        audit.setUri(req.getRequestURI());
        audit.setIp(ip);
        audit.setParams(toParamsJson(pjp.getArgs()));

        try {
            Object result = pjp.proceed();
            audit.setSuccess(1);
            audit.setCostMs((int) (System.currentTimeMillis() - start));
            return result;
        } catch (Throwable e) {
            // 接口失败也照记一笔（谁、什么操作、因为什么挂的），事后好追溯
            audit.setSuccess(0);
            audit.setErrorMsg(truncate(e.getMessage(), 500));
            audit.setCostMs((int) (System.currentTimeMillis() - start));
            throw e;          // 异常继续往上抛，交给全局异常处理器转成统一 JSON
        } finally {
            saveAuditQuietly(audit);   // 无论成败都落库；落库本身不能影响业务
        }
    }

    /**
     * 把接口入参转成 JSON 文本存审计表。有几点小心思：
     * 1. HttpServletRequest / MultipartFile 这类框架对象不能（也不该）序列化，跳过
     * 2. 登录密码绝不能进表——审计表泄露 = 密码泄露，password 打码成 ******
     * 3. 超长截断：TEXT 字段虽够大，但没必要的长度别往表里塞
     */
    private String toParamsJson(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        List<Object> safeArgs = new ArrayList<>(args.length);
        for (Object arg : args) {
            if (arg == null) {
                safeArgs.add(null);
                continue;
            }
            // 框架对象直接跳过（文件内容是二进制，存了也没法看）
            if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse
                    || arg instanceof MultipartFile) {
                continue;
            }
            if (arg instanceof LoginDTO login) {
                // 登录参数：用户名照记，密码打码，绝不明文入库
                Map<String, Object> safe = new HashMap<>();
                safe.put("username", login.getUsername());
                safe.put("password", "******");
                safeArgs.add(safe);
                continue;
            }
            safeArgs.add(arg);
        }
        String json;
        try {
            json = JSON.toJSONString(safeArgs);
        } catch (Exception e) {
            json = "[参数序列化失败]";   // 个别对象不支持序列化，不影响审计主流程
        }
        return truncate(json, 2000);
    }

    /** 截断超长字符串：超过 maxLen 就砍掉并加省略号 */
    private String truncate(String s, int maxLen) {
        if (s == null) {
            return null;
        }
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    /** 审计落库：自己失败只打警告日志，绝不能让"记审计"拖垮正常业务 */
    private void saveAuditQuietly(AuditLog audit) {
        try {
            auditLogMapper.insert(audit);
        } catch (Exception e) {
            log.warn("审计日志落库失败: {}", e.getMessage());
        }
    }
}
