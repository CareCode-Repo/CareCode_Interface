package com.carecode.domain.community.dto;

import com.carecode.domain.community.entity.Comment;
import com.carecode.domain.community.entity.Post;
import com.carecode.domain.community.entity.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 커뮤니티 관련 응답 DTO 통합 클래스
 */
public class CommunityResponse {

    /**
     * 게시글 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PostResponse {
        private Long postId;
        private String title;
        private String content;
        private String category;
        private String authorName;
        private String authorId;
        private Boolean isAnonymous;
        private String createdAt;
        private Integer viewCount;
        private Integer likeCount;
        private Integer commentCount;
        private List<String> tags;
        private Boolean isLiked;
        private Boolean isBookmarked;
    }

    /**
     * 게시글 상세 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostDetailResponse extends PostResponse {
        private List<CommentResponse> comments;
        private List<RelatedPostResponse> relatedPosts;
    }

    /**
     * 댓글 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CommentResponse {
        private Long commentId;
        private String content;
        private String authorName;
        private String authorId;
        private String createdAt;
        private Integer likeCount;
        private Boolean isLiked;
        private Long parentCommentId;
        private List<CommentResponse> replies;
    }

    /**
     * 관련 게시글 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RelatedPostResponse {
        private Long postId;
        private String title;
        private String category;
        private Integer viewCount;
        private Integer likeCount;
    }

    /**
     * 커뮤니티 통계 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CommunityStatsResponse {
        private Integer totalPosts;
        private Integer totalComments;
        private Integer totalUsers;
        private Map<String, Integer> categoryDistribution;
        private Map<String, Integer> dailyPostCount;
    }

    /**
     * 태그 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TagResponse {
        private Long id;
        private String name;
        private String description;
        private String createdAt;
    }

    /**
     * 페이지 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PageResponse<T> {
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

    /**
     * 게시글 목록 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PostList {
        private List<Post> posts;
        private long totalCount;
        private int currentPage;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
        private String category;
        private List<String> tags;
    }

    /**
     * 게시글 검색 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PostSearch {
        private List<Post> posts;
        private long totalCount;
        private String searchKeyword;
        private List<String> searchFilters;
        private String sortBy;
        private String sortDirection;
    }

    /**
     * 댓글 목록 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CommentList {
        private List<Comment> comments;
        private long totalCount;
        private int currentPage;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }

    /**
     * 태그 목록 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TagList {
        private List<Tag> tags;
        private long totalCount;
        private List<String> popularTags;
    }
}
