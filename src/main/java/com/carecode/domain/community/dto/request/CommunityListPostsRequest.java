package com.carecode.domain.community.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 게시글 목록 조회 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityListPostsRequest {
    private String category;
    private List<String> tags;
    private String sortBy;
    private String sortDirection;
    private int page;
    private int size;
}

