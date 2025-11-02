package com.carecode.domain.careFacility.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 육아 시설 예약 엔티티
 */
@Entity
@Table(name = "care_facility_bookings")
@EntityListeners(AuditingEntityListener.class)
public class CareFacilityBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private CareFacility facility;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "child_name", nullable = false)
    private String childName;

    @Column(name = "child_age")
    private Integer childAge;

    @Column(name = "parent_name", nullable = false)
    private String parentName;

    @Column(name = "parent_phone", nullable = false)
    private String parentPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_type", nullable = false)
    private BookingType bookingType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "special_requirements", columnDefinition = "TEXT")
    private String specialRequirements;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;

    @Column(name = "actual_end_time")
    private LocalDateTime actualEndTime;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 기본 생성자
    public CareFacilityBooking() {}

    // 빌더 생성자
    public CareFacilityBooking(Long id, CareFacility facility, String userId, String childName, Integer childAge,
                              String parentName, String parentPhone, BookingType bookingType, BookingStatus status,
                              LocalDateTime startTime, LocalDateTime endTime, String specialRequirements, String notes,
                              String cancellationReason, LocalDateTime cancelledAt, LocalDateTime actualStartTime,
                              LocalDateTime actualEndTime, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.facility = facility;
        this.userId = userId;
        this.childName = childName;
        this.childAge = childAge;
        this.parentName = parentName;
        this.parentPhone = parentPhone;
        this.bookingType = bookingType;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.specialRequirements = specialRequirements;
        this.notes = notes;
        this.cancellationReason = cancellationReason;
        this.cancelledAt = cancelledAt;
        this.actualStartTime = actualStartTime;
        this.actualEndTime = actualEndTime;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getter 메서드들
    public Long getId() { return id; }
    public CareFacility getFacility() { return facility; }
    public String getUserId() { return userId; }
    public String getChildName() { return childName; }
    public Integer getChildAge() { return childAge; }
    public String getParentName() { return parentName; }
    public String getParentPhone() { return parentPhone; }
    public BookingType getBookingType() { return bookingType; }
    public BookingStatus getStatus() { return status; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public String getSpecialRequirements() { return specialRequirements; }
    public String getNotes() { return notes; }
    public String getCancellationReason() { return cancellationReason; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public LocalDateTime getActualStartTime() { return actualStartTime; }
    public LocalDateTime getActualEndTime() { return actualEndTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setter 메서드들
    public void setId(Long id) { this.id = id; }
    public void setFacility(CareFacility facility) { this.facility = facility; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setChildName(String childName) { this.childName = childName; }
    public void setChildAge(Integer childAge) { this.childAge = childAge; }
    public void setParentName(String parentName) { this.parentName = parentName; }
    public void setParentPhone(String parentPhone) { this.parentPhone = parentPhone; }
    public void setBookingType(BookingType bookingType) { this.bookingType = bookingType; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public void setSpecialRequirements(String specialRequirements) { this.specialRequirements = specialRequirements; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public void setActualStartTime(LocalDateTime actualStartTime) { this.actualStartTime = actualStartTime; }
    public void setActualEndTime(LocalDateTime actualEndTime) { this.actualEndTime = actualEndTime; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    /**
     * 예약 취소
     */
    public void cancel(String reason) {
        this.status = BookingStatus.CANCELLED;
        this.cancellationReason = reason;
        this.cancelledAt = LocalDateTime.now();
    }

    /**
     * 예약 확정
     */
    public void confirm() {
        this.status = BookingStatus.CONFIRMED;
    }

    /**
     * 예약 완료
     */
    public void complete() {
        this.status = BookingStatus.COMPLETED;
        this.actualStartTime = this.startTime;
        this.actualEndTime = this.endTime;
    }

    /**
     * 예약 유형 열거형
     */
    public enum BookingType {
        VISIT("방문"),
        REGULAR("정기"),
        TEMPORARY("임시");

        private final String displayName;

        BookingType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 예약 상태 열거형
     */
    public enum BookingStatus {
        PENDING("대기중"),
        CONFIRMED("확정"),
        CANCELLED("취소됨"),
        COMPLETED("완료");

        private final String displayName;

        BookingStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
} 