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
public class ExceptionLogAspect {

    @Pointcut("execution(* com.debs.visualization.infrastructure.error.handler.GlobalExceptionHandler.handleMethodArgumentNotValidException(..))")
    public void methodArgumentNotValidExceptionPointCut() {
    }

    @Pointcut("execution(* com.debs.visualization.infrastructure.error.handler.GlobalExceptionHandler.handleMethodArgumentTypeMismatchException(..))")
    public void methodArgumentTypeMismatchExceptionPointCut() {
    }

    @Pointcut("execution(* com.debs.visualization.infrastructure.error.handler.GlobalExceptionHandler.handleHttpRequestMethodNotSupportedException(..))")
    public void httpRequestMethodNotSupportedExceptionPointCut() {
    }

    @Pointcut("execution(* com.debs.visualization.infrastructure.error.handler.GlobalExceptionHandler.handleRuntimeException(..))")
    public void runtimeExceptionPointCut() {
    }

    @Pointcut("execution(* com.debs.visualization.infrastructure.error.handler.GlobalExceptionHandler.handleUserDefineException(..))")
    public void userDefineExceptionPointCut() {
    }

    @Pointcut("execution(* com.debs.visualization.infrastructure.error.handler.GlobalExceptionHandler.handleIllegalStateException(..))")
    public void illegalStateExceptionPointCut() {
    }

    @Pointcut("execution(* com.debs.visualization.infrastructure.error.handler.GlobalExceptionHandler.handleException(..))")
    public void exceptionPointCut() {
    }

    @Around(value = "exceptionPointCut()")
    public Object printErrorLog(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        String methodName = joinPoint.getSignature().toShortString();
        final Servlets servlets = ServletExtractor.get(RequestContextHolder.currentRequestAttributes());
        final ResponseFormat<?> response = ResponseFormat.of(result);

        try {
            final LogScheme logScheme = new LogScheme(LogType.EXCEPTION, servlets, response, methodName);
            log.error("{}", logScheme);
        } catch (Exception e) {
            log.error("ExceptionLogAspect error : " + e.getMessage());
        }

        return result;
    }

    @Around(value = "methodArgumentNotValidExceptionPointCut() || "
                    + "methodArgumentTypeMismatchExceptionPointCut() || "
                    + "httpRequestMethodNotSupportedExceptionPointCut() || "
                    + "runtimeExceptionPointCut() || "
                    + "userDefineExceptionPointCut() || "
                    + "illegalStateExceptionPointCut()")
    public Object printWarnLog(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        String methodName = joinPoint.getSignature().toShortString();
        final Servlets servlets = ServletExtractor.get(RequestContextHolder.currentRequestAttributes());
        final ResponseFormat<?> response = ResponseFormat.of(result);

        try {
            final LogScheme logScheme = new LogScheme(LogType.EXCEPTION, servlets, response, methodName);
            log.warn("{}", logScheme);
        } catch (Exception e) {
            log.error("ExceptionLogAspect error : " + e.getMessage());
        }

        return result;
    }
}
