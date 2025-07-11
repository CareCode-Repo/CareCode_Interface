package com.carecode.domain.careFacility.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "facility_schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class FacilitySchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private CareFacility careFacility;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    @Column(name = "is_closed", nullable = false)
    private Boolean isClosed = false;

    @Column(name = "break_start_time")
    private LocalTime breakStartTime;

    @Column(name = "break_end_time")
    private LocalTime breakEndTime;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public FacilitySchedule(CareFacility careFacility, DayOfWeek dayOfWeek, 
                          LocalTime openTime, LocalTime closeTime, Boolean isClosed,
                          LocalTime breakStartTime, LocalTime breakEndTime, String description) {
        this.careFacility = careFacility;
        this.dayOfWeek = dayOfWeek;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.isClosed = isClosed;
        this.breakStartTime = breakStartTime;
        this.breakEndTime = breakEndTime;
        this.description = description;
    }

    public void updateSchedule(LocalTime openTime, LocalTime closeTime, Boolean isClosed,
                             LocalTime breakStartTime, LocalTime breakEndTime, String description) {
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.isClosed = isClosed;
        this.breakStartTime = breakStartTime;
        this.breakEndTime = breakEndTime;
        this.description = description;
    }

    public void setClosed() {
        this.isClosed = true;
    }

    public void setOpen() {
        this.isClosed = false;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }
} 