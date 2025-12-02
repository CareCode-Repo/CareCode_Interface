package com.carecode.domain.community.dto.response;

import com.carecode.domain.community.entity.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 태그 목록 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityTagListResponse {
    private List<Tag> tags;
    private long totalCount;
    private List<String> popularTags;
}

