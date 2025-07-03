package com.carecode.core.aspect;

import com.carecode.core.annotation.ValidateChildAge;
import com.carecode.core.annotation.ValidateLocation;
import com.carecode.core.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ValidationAspect {

    @Before("@annotation(validateLocation)")
    public void validateLocation(JoinPoint joinPoint, ValidateLocation validateLocation) {
        Object[] args = joinPoint.getArgs();
        
        // 위치 정보 검증 로직
        boolean hasLocation = false;
        for (Object arg : args) {
            if (arg != null && (arg.toString().contains("latitude") || arg.toString().contains("longitude"))) {
                hasLocation = true;
                break;
            }
        }
        
        if (validateLocation.required() && !hasLocation) {
            throw new BusinessException(validateLocation.message());
        }
    }

    @Before("@annotation(validateChildAge)")
    public void validateChildAge(JoinPoint joinPoint, ValidateChildAge validateChildAge) {
        Object[] args = joinPoint.getArgs();
        
        // 자녀 연령 검증 로직
        for (Object arg : args) {
            if (arg instanceof Integer) {
                int age = (Integer) arg;
                if (age < validateChildAge.minAge() || age > validateChildAge.maxAge()) {
                    throw new BusinessException(validateChildAge.message());
                }
            }
        }
    }
} 