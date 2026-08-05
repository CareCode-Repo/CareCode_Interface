package com.carecode.domain.health.dto.response;

import lombok.Builder;
import lombok.Getter;

/** 병원 좋아요(찜) 상태 응답. 여부와 총 개수를 한 번에 내려 요청을 두 번 하지 않게 한다. */
@Getter
@Builder
public class HospitalLikeStatusResponse {

    private final Long hospitalId;

    /** 현재 로그인한 사용자가 이 병원을 찜했는지 여부 */
    private final boolean liked;

    private final long likeCount;
}
