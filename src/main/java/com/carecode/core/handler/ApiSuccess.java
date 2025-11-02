package com.carecode.core.handler;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiSuccess {
    private Date timestamp;
    private String message;
    
    /**
     * 간편한 ApiSuccess 객체 생성을 위한 정적 팩토리 메서드
     * @param message 성공 메시지
     * @return ApiSuccess 객체
     */
    public static ApiSuccess of(String message) {
        return ApiSuccess.builder()
                .timestamp(new Date())
                .message(message)
                .build();
    }
}


