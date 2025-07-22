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
 * 커뮤니티 댓글 엔티티
 * 계층형 댓글 구조를 지원 (답글/대댓글 기능)
 */
@Entity
@Table(name = "TBL_COMMENT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;
    
    @Column(name = "author_name")
    private String authorName;
    
    @Column(name = "like_count")
    private Integer likeCount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CommentStatus status;
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 계층형 댓글 구조를 위한 self-referencing
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment; // 부모 댓글 (답글인 경우)
    
    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> replies = new ArrayList<>(); // 답글 목록
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (likeCount == null) likeCount = 0;
        if (isActive == null) isActive = true;
        if (status == null) status = CommentStatus.PUBLISHED;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * 댓글에 답글 추가
     */
    public void addReply(Comment reply) {
        replies.add(reply);
        reply.setParentComment(this);
    }
    
    /**
     * 댓글에서 답글 제거
     */
    public void removeReply(Comment reply) {
        replies.remove(reply);
        reply.setParentComment(null);
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
     * 댓글 상태 변경
     */
    public void updateStatus(CommentStatus status) {
        this.status = status;
        if (status == CommentStatus.DELETED) {
            this.isActive = false;
        }
    }
    
    /**
     * 댓글 내용 업데이트
     */
    public void updateContent(String content) {
        this.content = content;
    }
    
    /**
     * 댓글인지 확인 (답글이 아닌 최상위 댓글)
     */
    public boolean isTopLevelComment() {
        return parentComment == null;
    }
    
    /**
     * 답글인지 확인
     */
    public boolean isReply() {
        return parentComment != null;
    }
    
    /**
     * 댓글 상태 Enum
     */
    public enum CommentStatus {
        PUBLISHED("발행"),
        HIDDEN("숨김"),
        DELETED("삭제");
        
        private final String displayName;
        
        CommentStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
} 