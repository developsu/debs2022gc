package com.debs.visualization.infrastructure.logging.aspect;

import com.debs.visualization.infrastructure.logging.util.ServletExtractor;
import com.debs.visualization.infrastructure.logging.util.ServletExtractor.Servlets;
import com.debs.visualization.infrastructure.http.ResponseFormat;
import com.debs.visualization.infrastructure.logging.model.LogScheme;
import com.debs.visualization.infrastructure.logging.model.LogType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

@Slf4j
@Component
@Aspect
public class RequestLogAspect {

    @Pointcut("execution(* com.debs.visualization.interfaces.controller.*.*(..))")
    public void logPointCut() {
    }

    @Around("logPointCut()")
    public Object printRequestLog(ProceedingJoinPoint joinPoint) throws Throwable {
        final Object result = joinPoint.proceed();
        final String methodName = joinPoint.getSignature().toShortString();
        final Servlets servlets = ServletExtractor.get(RequestContextHolder.currentRequestAttributes());
        final ResponseFormat<?> response = ResponseFormat.of(result);

        try {
            final LogScheme logScheme = new LogScheme(LogType.REQUEST, servlets, response, methodName);
            log.info("{}", logScheme);
        } catch (Exception e) {
            log.error("RequestLogAspect error : " + e.getMessage());
        }

        return result;
    }
}
