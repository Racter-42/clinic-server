package com.itheima.clinicserver.aspect;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Aspect
@Component
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);

    @Around("execution(* com.itheima.clinicserver.controller..*.*(..))")
    public Object audit(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();

        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        HttpServletRequest req = ((ServletRequestAttributes) attrs).getRequest();
        String ip = req.getRemoteAddr();

        String user = (String) req.getAttribute("userId");
        if (user == null) {
            user = "anonymous";
        }

        String method = pjp.getSignature().getDeclaringType().getSimpleName()
                + "." + pjp.getSignature().getName();
        Object[] args = pjp.getArgs();

        Object result;
        try {
            result = pjp.proceed();
            log.info("[AUDIT] user={} ip={} method={} args={} result={} cost={}ms",
                    user, ip, method, Arrays.toString(args), result,
                    System.currentTimeMillis() - start);
            return result;
        } catch (Throwable e) {
            log.error("[AUDIT-ERR] user={} ip={} method={} args={} err={}",
                    user, ip, method, Arrays.toString(args), e.getMessage(), e);
            throw e;
        }
    }
}