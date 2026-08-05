package com.carecode.domain.health.entity;

import com.carecode.domain.user.entity.Child;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 아이별 예방접종 일정. 아이를 등록하면 VaccineType 표준 일정에 따라 접종 예정일이 자동으로 생성된다. */
@Entity
@Table(
    name = "TBL_VACCINATION_SCHEDULE",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_vaccination_child_vaccine_dose",
        columnNames = {"child_id", "vaccine_type", "dose_number"}
    ),
    indexes = {
        @Index(name = "idx_vaccination_child", columnList = "child_id"),
        @Index(name = "idx_vaccination_due_date", columnList = "due_date"),
        @Index(name = "idx_vaccination_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaccinationSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @Enumerated(EnumType.STRING)
    @Column(name = "vaccine_type", nullable = false, length = 50)
    private VaccineType vaccineType;

    /** 같은 백신의 몇 차 접종인지 (1부터). */
    @Column(name = "dose_number", nullable = false)
    private Integer doseNumber;

    /** 표준 일정상 접종 예정일. */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /** 실제 접종일. 완료 처리 시 기록된다. */
    @Column(name = "completed_date")
    private LocalDate completedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private VaccinationStatus status = VaccinationStatus.SCHEDULED;

    /** 사전 알림을 이미 보냈는지. 중복 발송을 막는다. */
    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = VaccinationStatus.SCHEDULED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void markCompleted(LocalDate completedDate) {
        this.completedDate = completedDate;
        this.status = VaccinationStatus.COMPLETED;
    }

    public void markReminderSent() {
        this.reminderSentAt = LocalDateTime.now();
    }

    /** 예정일이 지났는데 아직 접종하지 않은 상태인지. */
    public boolean isOverdue(LocalDate today) {
        return status == VaccinationStatus.SCHEDULED && dueDate.isBefore(today);
    }

    public enum VaccinationStatus {
        SCHEDULED("예정"),
        COMPLETED("완료"),
        SKIPPED("건너뜀");

        private final String displayName;

        VaccinationStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
