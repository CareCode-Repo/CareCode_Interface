package com.carecode.domain.health.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 병원 정보 응답 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HospitalInfoResponse {
    private Long id;
    private String name;

    /** 진료과목 (소아청소년과 등) */
    private String type;

    /**
     * 요양기관 종별 (의원/병원/종합병원/상급종합).
     * 동네 소아과와 대학병원은 부모의 선택 기준이 달라 진료과목과 분리해 노출한다.
     */
    private String grade;

    private String address;
    private String phoneNumber;
    private Double latitude;
    private Double longitude;
    private String createdAt;
    private String updatedAt;
}

