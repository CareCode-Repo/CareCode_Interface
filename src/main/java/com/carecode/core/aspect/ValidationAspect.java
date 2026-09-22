package com.carecode.core.aspect;

import com.carecode.core.annotation.ValidateChildAge;
import com.carecode.core.annotation.ValidateLocation;
import com.carecode.core.exception.BusinessException;
import com.carecode.core.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ValidationAspect {

    /**
     * 좌표를 받는 메서드면 좌표가 있고 범위 안인지 확인한다.
     *
     * <p>예전 구현은 인자의 {@code toString()} 에 "latitude" 라는 글자가 있는지를 봤다.
     * 좌표 인자는 {@code 37.5} 같은 숫자라 그 글자가 나올 수 없고, 검색 DTO 는 toString 이 없고,
     * 지역명은 그냥 문자열이라, 이 어노테이션이 붙은 API(반경 검색·지역별 조회·시설 검색·지역별 정책)가
     * 입력과 무관하게 전부 400 이었다.
     *
     * <p>이제 파라미터 이름이 {@code latitude}/{@code longitude} 인 인자만 좌표로 본다.
     * 좌표를 받지 않는 메서드(지역명·검색 조건)에는 확인할 게 없으므로 통과시킨다.
     */
    @Before("@annotation(validateLocation)")
    public void validateLocation(JoinPoint joinPoint, ValidateLocation validateLocation) {
        String[] names = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
        Object[] args = joinPoint.getArgs();
        if (names == null) {
            return;
        }

        boolean takesCoordinates = false;
        Double latitude = null;
        Double longitude = null;
        for (int i = 0; i < names.length && i < args.length; i++) {
            if ("latitude".equals(names[i])) {
                takesCoordinates = true;
                latitude = args[i] instanceof Number n ? n.doubleValue() : null;
            } else if ("longitude".equals(names[i])) {
                takesCoordinates = true;
                longitude = args[i] instanceof Number n ? n.doubleValue() : null;
            }
        }
        if (!takesCoordinates) {
            return;
        }

        if (latitude == null || longitude == null) {
            if (validateLocation.required()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, validateLocation.message());
            }
            return;
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "위도·경도 범위가 올바르지 않습니다.");
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