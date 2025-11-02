package com.carecode.domain.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 커뮤니티 관련 요청 DTO 통합 클래스
 */
public class CommunityRequest {

    /**
     * 게시글 작성 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreatePost {
        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 100, message = "제목은 100자 이하여야 합니다")
        private String title;
        
        @NotBlank(message = "내용은 필수입니다")
        @Size(max = 2000, message = "내용은 2000자 이하여야 합니다")
        private String content;
        
        private String category;
        private List<String> tags;
        private boolean isAnonymous;
    }

    /**
     * 게시글 수정 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdatePost {
        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 100, message = "제목은 100자 이하여야 합니다")
        private String title;
        
        @NotBlank(message = "내용은 필수입니다")
        @Size(max = 2000, message = "내용은 2000자 이하여야 합니다")
        private String content;
        
        private String category;
        private List<String> tags;
    }

    /**
     * 댓글 작성 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateComment {
        @NotBlank(message = "댓글 내용은 필수입니다")
        @Size(max = 500, message = "댓글은 500자 이하여야 합니다")
        private String content;
        
        private Long parentCommentId;
        private boolean isAnonymous;
    }

    /**
     * 댓글 수정 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateComment {
        @NotBlank(message = "댓글 내용은 필수입니다")
        @Size(max = 500, message = "댓글은 500자 이하여야 합니다")
        private String content;
    }

    /**
     * 태그 생성 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateTag {
        @NotBlank(message = "태그 이름은 필수입니다")
        @Size(max = 20, message = "태그 이름은 20자 이하여야 합니다")
        private String name;
        
        private String description;
        private String color;
    }

    /**
     * 게시글 검색 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SearchPosts {
        private String keyword;
        private String category;
        private List<String> tags;
        private String sortBy;
        private String sortDirection;
        private int page;
        private int size;
    }

    /**
     * 게시글 목록 조회 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListPosts {
        private String category;
        private List<String> tags;
        private String sortBy;
        private String sortDirection;
        private int page;
        private int size;
    }
}
