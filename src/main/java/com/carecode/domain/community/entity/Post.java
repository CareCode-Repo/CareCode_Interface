package com.carecode.domain.community.entity;

import com.carecode.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 커뮤니티 게시글 엔티티
 */
@Entity
@Table(name = "TBL_POST")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "title", nullable = false)
    private String title;
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;
    
    @Column(name = "author_name")
    private String authorName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private PostCategory category;
    
    @Column(name = "is_anonymous")
    private Boolean isAnonymous;
    
    @Column(name = "view_count")
    private Integer viewCount;
    
    @Column(name = "like_count")
    private Integer likeCount;
    
    @Column(name = "comment_count")
    private Integer commentCount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PostStatus status;
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();
    
    @ManyToMany
    @JoinTable(
        name = "TBL_POST_TAGS",
        joinColumns = @JoinColumn(name = "POST_ID"),
        inverseJoinColumns = @JoinColumn(name = "TAG_ID")
    )
    private List<Tag> tags = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (viewCount == null) viewCount = 0;
        if (likeCount == null) likeCount = 0;
        if (commentCount == null) commentCount = 0;
        if (isActive == null) isActive = true;
        if (isAnonymous == null) isAnonymous = false;
        if (status == null) status = PostStatus.PUBLISHED;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * 게시글에 댓글 추가
     */
    public void addComment(Comment comment) {
        comments.add(comment);
        comment.setPost(this);
        this.commentCount = comments.size();
    }
    
    /**
     * 게시글에서 댓글 제거
     */
    public void removeComment(Comment comment) {
        comments.remove(comment);
        comment.setPost(null);
        this.commentCount = comments.size();
    }
    
    /**
     * 조회수 증가
     */
    public void incrementViewCount() {
        this.viewCount++;
    }
    
    /**
     * 좋아요 수 증가
     */
    public void incrementLikeCount() {
        this.likeCount++;
    }
    
    /**
     * 좋아요 수 감소
     */
    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }
    
    /**
     * 게시글 상태 변경
     */
    public void updateStatus(PostStatus status) {
        this.status = status;
        if (status == PostStatus.DELETED) {
            this.isActive = false;
        }
    }
    
    /**
     * 태그 추가
     */
    public void addTag(Tag tag) {
        if (!tags.contains(tag)) {
            tags.add(tag);
        }
    }
    
    /**
     * 태그 제거
     */
    public void removeTag(Tag tag) {
        tags.remove(tag);
    }
    
    /**
     * 모든 태그 제거
     */
    public void clearTags() {
        tags.clear();
    }
    
    /**
     * 게시글 카테고리 Enum
     */
    public enum PostCategory {
        GENERAL("일반"),
        QUESTION("질문"),
        SHARE("공유"),
        REVIEW("후기"),
        NEWS("뉴스"),
        EVENT("이벤트"),
        NOTICE("공지사항");
        
        private final String displayName;
        
        PostCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 게시글 상태 Enum
     */
    public enum PostStatus {
        DRAFT("임시저장"),
        PUBLISHED("발행"),
        HIDDEN("숨김"),
        DELETED("삭제");
        
        private final String displayName;
        
        PostStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
} 