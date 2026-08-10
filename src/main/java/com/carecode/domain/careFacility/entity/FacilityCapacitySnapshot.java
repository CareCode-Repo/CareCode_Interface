package com.carecode.domain.careFacility.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 특정 시점의 시설 정원·현원 관측값. 한 번 쓰면 고치지 않는 append-only 기록이다. */
@Entity
@Table(name = "TBL_FACILITY_CAPACITY_SNAPSHOT",
        uniqueConstraints = @UniqueConstraint(name = "UK_FACILITY_SNAPSHOT_DATE",
                columnNames = {"FACILITY_ID", "OBSERVED_DATE"}))
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FacilityCapacitySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "FACILITY_ID", nullable = false)
    private Long facilityId;

    @Column(name = "OBSERVED_DATE", nullable = false)
    private LocalDate observedDate;

    @Column(name = "CAPACITY")
    private Integer capacity;

    @Column(name = "CURRENT_ENROLLMENT")
    private Integer currentEnrollment;

    @Column(name = "AVAILABLE_SPOTS")
    private Integer availableSpots;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /** 같은 날 재동기화 시 최신 관측값으로 맞춘다. 날짜별로 한 행만 유지한다. */
    public void refresh(Integer capacity, Integer currentEnrollment, Integer availableSpots) {
        this.capacity = capacity;
        this.currentEnrollment = currentEnrollment;
        this.availableSpots = availableSpots;
    }
}
