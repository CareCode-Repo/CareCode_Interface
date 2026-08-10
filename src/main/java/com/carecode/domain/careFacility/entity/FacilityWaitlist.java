package com.carecode.domain.careFacility.entity;

import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** 대기 신청부터 입소까지의 기록. 공공데이터에 없는 "실제 대기 기간" 의 유일한 출처다. */
@Entity
@Table(name = "TBL_FACILITY_WAITLIST",
        uniqueConstraints = @UniqueConstraint(name = "UK_WAITLIST_CHILD_FACILITY",
                columnNames = {"FACILITY_ID", "CHILD_ID"}))
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FacilityWaitlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "FACILITY_ID", nullable = false)
    private Long facilityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CHILD_ID", nullable = false)
    private Child child;

    @Column(name = "WAIT_NUMBER")
    private Integer waitNumber;

    @Column(name = "APPLIED_AT", nullable = false)
    private LocalDate appliedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private WaitStatus status;

    @Column(name = "RESOLVED_AT")
    private LocalDate resolvedAt;

    /** 신청 당시 월령. 0세반과 3세반은 대기 양상이 완전히 다르다. */
    @Column(name = "CLASS_AGE")
    private Integer classAge;

    @Column(name = "NOTE", length = 300)
    private String note;

    /** 마지막으로 빈자리를 알린 관측일. 같은 자리로 반복 발송하지 않기 위한 기준이다. */
    @Column(name = "VACANCY_NOTIFIED_AT")
    private LocalDate vacancyNotifiedAt;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    public enum WaitStatus {
        WAITING("대기 중"),
        ADMITTED("입소"),
        GAVE_UP("포기");

        private final String displayName;

        WaitStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = WaitStatus.WAITING;
        }
    }

    /** 입소·포기 처리. 이 시점이 찍혀야 대기 기간이 계산된다. */
    public void resolve(WaitStatus status, LocalDate resolvedAt, String note) {
        this.status = status;
        this.resolvedAt = resolvedAt != null ? resolvedAt : LocalDate.now();
        this.note = note;
        this.updatedAt = LocalDateTime.now();
    }

    /** 빈자리를 알렸음을 기록한다. */
    public void markVacancyNotified(LocalDate observedDate) {
        this.vacancyNotifiedAt = observedDate;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 이 관측일의 빈자리를 알려도 되는지.
     *
     * <p>같은 자리를 두 번 알리면 신뢰를 잃고, 정원이 오르내릴 때마다 알리면 스팸이 된다.
     * 마지막 발송 이후 최소 간격이 지나야 다시 보낸다.
     */
    public boolean canNotifyVacancy(LocalDate observedDate, int minIntervalDays) {
        if (status != WaitStatus.WAITING) {
            return false;
        }
        if (vacancyNotifiedAt == null) {
            return true;
        }
        return ChronoUnit.DAYS.between(vacancyNotifiedAt, observedDate) >= minIntervalDays;
    }

    /** 대기 일수. 아직 대기 중이면 오늘까지로 센다. */
    public long waitedDays() {
        LocalDate end = resolvedAt != null ? resolvedAt : LocalDate.now();
        return ChronoUnit.DAYS.between(appliedAt, end);
    }
}
