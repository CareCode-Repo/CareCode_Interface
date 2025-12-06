package com.carecode.core.aspect;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.util.LoggingUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

/**
 * 메서드 실행 시간 로깅을 위한 Aspect
 * @LogExecutionTime 어노테이션이 붙은 메서드의 실행 시간을 측정하고 로깅합니다.
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around("@annotation(logExecutionTime)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint, LogExecutionTime logExecutionTime) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethodName = className + "." + methodName;
        
        // MDC에 메서드 정보 추가
        MDC.put("method", fullMethodName);
        if (MDC.get("traceId") == null) {
            LoggingUtil.setTraceId(null);
        }
        
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();
            
            long executionTime = stopWatch.getTotalTimeMillis();
            MDC.put("executionTime", String.valueOf(executionTime));
            
            if (logExecutionTime.logArgs()) {
                Object[] args = joinPoint.getArgs();
                log.info("메서드 실행 완료 - 실행 시간: {}ms, 인자: {}", 
                    executionTime, formatArgs(args));
            } else {
                log.info("메서드 실행 완료 - 실행 시간: {}ms", executionTime);
            }
            
            // 경고 임계값 체크
            if (logExecutionTime.warnThreshold() > 0 && executionTime > logExecutionTime.warnThreshold()) {
                log.warn("실행 시간이 임계값({}ms)을 초과했습니다: {}ms", 
                    logExecutionTime.warnThreshold(), executionTime);
            }
            
            return result;
        } catch (Exception e) {
            stopWatch.stop();
            long executionTime = stopWatch.getTotalTimeMillis();
            MDC.put("executionTime", String.valueOf(executionTime));
            MDC.put("error", e.getClass().getSimpleName());
            
            log.error("메서드 실행 실패 - 실행 시간: {}ms, 오류: {}", 
                executionTime, e.getMessage(), e);
            
            throw e;
        } finally {
            // 메서드 관련 MDC 제거 (traceId는 유지)
            MDC.remove("method");
            MDC.remove("executionTime");
            MDC.remove("error");
        }
    }
    
    /**
     * 메서드 인자를 문자열로 포맷팅
     */
    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            
            Object arg = args[i];
            if (arg == null) {
                sb.append("null");
            } else if (arg instanceof String) {
                String str = (String) arg;
                // 민감한 정보 마스킹 (비밀번호, 토큰 등)
                if (str.length() > 50) {
                    sb.append(str.substring(0, 50)).append("...");
                } else {
                    sb.append(str);
                }
            } else {
                sb.append(arg.toString());
            }
        }
        sb.append("]");
        return sb.toString();
    }
} 