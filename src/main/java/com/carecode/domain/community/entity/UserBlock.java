package com.carecode.domain.community.entity;

import com.carecode.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 사용자 차단. 차단하면 상대의 게시글·댓글이 목록에서 보이지 않는다. */
@Entity
@Table(
    name = "TBL_USER_BLOCK",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_block",
        columnNames = {"blocker_id", "blocked_id"}
    ),
    indexes = @Index(name = "idx_user_block_blocker", columnList = "blocker_id")
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 차단한 사용자. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    /** 차단당한 사용자. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
