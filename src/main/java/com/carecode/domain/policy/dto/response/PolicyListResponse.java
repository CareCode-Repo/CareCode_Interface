package com.carecode.domain.policy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 정책 검색 응답.
 *
 * <p>항목은 다른 정책 목록 API 와 같은 {@link PolicyDto} 다. 전에는 검색만 PolicyInfoResponse 로
 * 옮겨 담아(id 가 문자열, 지역·금액 필드 이름이 다름) 프런트 스키마 파싱이 실패했고,
 * 검색 결과 화면은 늘 오류였다. 페이지 필드도 프런트가 읽는 이름(totalElements, pageSize)으로 준다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyListResponse {
    private List<PolicyDto> policies;
    private long totalElements;
    /** @deprecated totalElements 와 같다. 예전 이름을 쓰던 클라이언트를 위해 남긴다. */
    @Deprecated
    private long totalCount;
    private int currentPage;
    private int pageSize;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
    private String category;
    private String city;
    private String district;
}

