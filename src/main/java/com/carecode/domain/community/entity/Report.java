package com.carecode.domain.community.entity;

import com.carecode.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 게시글·댓글 신고.
 *
 * <p>같은 사용자가 같은 대상을 중복 신고하지 못하도록 유니크 제약을 둔다.
 */
@Entity
@Table(
    name = "TBL_REPORT",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_report_reporter_target",
        columnNames = {"reporter_id", "target_type", "target_id"}
    ),
    indexes = {
        @Index(name = "idx_report_status", columnList = "status"),
        @Index(name = "idx_report_target", columnList = "target_type,target_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private ReportReason reason;

    @Column(name = "detail", length = 1000)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    /** 관리자 처리 메모. */
    @Column(name = "moderator_note", length = 1000)
    private String moderatorNote;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = ReportStatus.PENDING;
        }
    }

    public void resolve(ReportStatus status, String note) {
        this.status = status;
        this.moderatorNote = note;
        this.resolvedAt = LocalDateTime.now();
    }

    public enum TargetType {
        POST("게시글"),
        COMMENT("댓글");

        private final String displayName;

        TargetType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum ReportReason {
        SPAM("스팸/광고"),
        ABUSE("욕설/비방"),
        SEXUAL("음란물"),
        PRIVACY("개인정보 노출"),
        FALSE_INFO("허위 정보"),
        OTHER("기타");

        private final String displayName;

        ReportReason(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum ReportStatus {
        PENDING("접수"),
        ACCEPTED("조치 완료"),
        REJECTED("반려");

        private final String displayName;

        ReportStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
