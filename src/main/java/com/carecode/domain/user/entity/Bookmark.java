package com.carecode.domain.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 북마크 엔티티
 * 사용자가 게시글, 정책, 시설 등을 북마크하는 기능을 관리
 */
@Entity
@Table(name = "TBL_BOOKMARK", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"USER_ID", "TARGET_TYPE", "TARGET_ID"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bookmark {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;
    
    @Column(name = "TARGET_TYPE", nullable = false)
    private String targetType; // POST, POLICY, FACILITY
    
    @Column(name = "TARGET_ID", nullable = false)
    private Long targetId;
    
    @Column(name = "NOTE")
    private String note;
    
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
    
    public boolean isForPolicy() {
        return "POLICY".equals(targetType);
    }
    
    public boolean isForFacility() {
        return "FACILITY".equals(targetType);
    }
    
    public String getTargetTypeDisplay() {
        switch (targetType) {
            case "POST":
                return "게시글";
            case "POLICY":
                return "정책";
            case "FACILITY":
                return "시설";
            default:
                return targetType;
        }
    }
    
    public boolean hasNote() {
        return note != null && !note.trim().isEmpty();
    }
    
    public String getNotePreview() {
        if (!hasNote()) return "";
        return note.length() > 50 ? note.substring(0, 50) + "..." : note;
    }
} 