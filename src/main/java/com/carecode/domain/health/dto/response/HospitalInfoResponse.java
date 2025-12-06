package com.carecode.domain.health.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 병원 정보 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HospitalInfoResponse {
    private Long id;
    private String name;
    private String type;
    private String address;
    private String phoneNumber;
    private Double latitude;
    private Double longitude;
    private String createdAt;
    private String updatedAt;
}

