package com.carecode.domain.community.dto.response;

import com.carecode.domain.community.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 게시글 검색 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityPostSearchResponse {
    private List<Post> posts;
    private long totalCount;
    private String searchKeyword;
    private List<String> searchFilters;
    private String sortBy;
    private String sortDirection;
}

