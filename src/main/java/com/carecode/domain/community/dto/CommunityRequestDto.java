package com.carecode.domain.community.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * 커뮤니티 요청 DTO들
 */
public class CommunityRequestDto {
    
    /**
     * 게시글 작성 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreatePostRequest {
        private String title;
        private String content;
        private String category;
        private List<String> tags;
        private Boolean isAnonymous;
    }
    
    /**
     * 게시글 수정 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdatePostRequest {
        private String title;
        private String content;
        private String category;
        private List<String> tags;
    }
    
    /**
     * 댓글 작성 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateCommentRequest {
        private String content;
        private Long parentCommentId;
    }
    
    /**
     * 댓글 수정 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateCommentRequest {
        private String content;
    }
} 