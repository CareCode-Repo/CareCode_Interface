package com.carecode.domain.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 페이지 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityPageResponse<T> {
    private List<T> content;           // 현재 페이지 데이터
    private int page;                  // 현재 페이지 번호 (0부터 시작)
    private int size;                  // 페이지당 항목 수
    private long totalElements;       // 전체 데이터 개수
    private int totalPages;           // 전체 페이지 수
    private boolean first;            // 첫 번째 페이지 여부
    private boolean last;             // 마지막 페이지 여부
    private boolean hasNext;          // 다음 페이지 존재 여부
    private boolean hasPrevious;      // 이전 페이지 존재 여부
}

