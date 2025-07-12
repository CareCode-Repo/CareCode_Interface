package com.carecode.domain.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 좋아요 엔티티
 * 게시글, 댓글, 리뷰 등에 대한 좋아요를 관리
 */
@Entity
@Table(name = "TBL_LIKE", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"USER_ID", "TARGET_TYPE", "TARGET_ID"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Like {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;
    
    @Column(name = "TARGET_TYPE", nullable = false)
    private String targetType; // POST, COMMENT, REVIEW
    
    @Column(name = "TARGET_ID", nullable = false)
    private Long targetId;
    
    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // 비즈니스 로직 메서드들
    public boolean isForPost() {
        return "POST".equals(targetType);
    }
    
    public boolean isForComment() {
        return "COMMENT".equals(targetType);
    }
    
    public boolean isForReview() {
        return "REVIEW".equals(targetType);
    }
    
    public String getTargetTypeDisplay() {
        switch (targetType) {
            case "POST":
                return "게시글";
            case "COMMENT":
                return "댓글";
            case "REVIEW":
                return "리뷰";
            default:
                return targetType;
        }
    }
} 