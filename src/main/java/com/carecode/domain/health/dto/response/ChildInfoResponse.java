package com.carecode.domain.health.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 아동 정보 응답 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChildInfoResponse {
    private Long id;
    private Long userId;
    private String name;
    private String birthDate;
    private String gender;
    /**
     * 알레르기·기저질환 등. 등록·수정 요청은 이 값을 받으면서 응답에는 없어서,
     * 수정 화면이 현재 값을 읽어올 방법이 없었다. 수정은 전체 교체라 그대로 두면
     * 다른 항목만 고쳐도 특이사항이 지워진다.
     */
    private String specialNeeds;
    private String createdAt;
    private String updatedAt;
}

