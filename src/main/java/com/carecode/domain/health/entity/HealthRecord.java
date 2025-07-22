package com.carecode.domain.health.entity;

import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 건강 기록 엔티티
 */
@Entity
@Table(name = "TBL_HEALTH_RECORD")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id")
    private Child child;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "record_type", nullable = false)
    private RecordType recordType;
    
    @Column(name = "title", nullable = false)
    private String title;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;
    
    @Column(name = "next_date")
    private LocalDate nextDate;
    
    @Column(name = "location")
    private String location;
    
    @Column(name = "doctor_name")
    private String doctorName;
    
    @Column(name = "hospital_name")
    private String hospitalName;
    
    @Column(name = "height")
    private Double height; // 키 (cm)
    
    @Column(name = "weight")
    private Double weight; // 몸무게 (kg)
    
    @Column(name = "temperature")
    private Double temperature; // 체온 (°C)
    
    @Column(name = "blood_pressure")
    private String bloodPressure; // 혈압 (예: 120/80)
    
    @Column(name = "pulse_rate")
    private Integer pulseRate; // 맥박 (회/분)
    
    @Column(name = "vaccine_name")
    private String vaccineName; // 예방접종명
    
    @Column(name = "vaccine_batch")
    private String vaccineBatch; // 백신 배치번호
    
    @Column(name = "symptoms", columnDefinition = "TEXT")
    private String symptoms; // 증상
    
    @Column(name = "diagnosis", columnDefinition = "TEXT")
    private String diagnosis; // 진단
    
    @Column(name = "treatment", columnDefinition = "TEXT")
    private String treatment; // 치료
    
    @Column(name = "medication", columnDefinition = "TEXT")
    private String medication; // 처방약
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes; // 기타 메모
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RecordStatus status;
    
    @Column(name = "is_completed")
    private Boolean isCompleted;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_type_id", insertable = false, updatable = false)
    private HealthRecordType healthRecordType;
    
    @OneToMany(mappedBy = "healthRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HealthRecordAttachment> healthRecordAttachments = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isCompleted == null) {
            isCompleted = false;
        }
        if (status == null) {
            status = RecordStatus.COMPLETED;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * 기록 완료 처리
     */
    public void markAsCompleted() {
        this.isCompleted = true;
        this.status = RecordStatus.COMPLETED;
    }
    
    /**
     * 기록 상태 업데이트
     */
    public void updateStatus(RecordStatus status) {
        this.status = status;
        if (status == RecordStatus.COMPLETED) {
            this.isCompleted = true;
        }
    }
    
    /**
     * 기록 내용 업데이트
     */
    public void updateRecord(LocalDate recordDate,
                             Double height, Double weight, Double temperature,
                             String bloodPressure, Integer pulseRate, String notes) {
        this.title = title;
        this.description = description;
        this.recordDate = recordDate;
        this.location = location;
        this.doctorName = doctorName;
        this.hospitalName = hospitalName;
        this.height = height;
        this.weight = weight;
        this.temperature = temperature;
        this.bloodPressure = bloodPressure;
        this.pulseRate = pulseRate;
        this.notes = notes;
    }
    
    /**
     * 기록 타입 Enum
     */
    public enum RecordType {
        VACCINATION("예방접종"),
        CHECKUP("건강검진"),
        ILLNESS("질병"),
        GROWTH("성장기록"),
        DENTAL("치과"),
        EYE("안과"),
        EMERGENCY("응급"),
        OTHER("기타");
        
        private final String displayName;
        
        RecordType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 기록 상태 Enum
     */
    public enum RecordStatus {
        SCHEDULED("예정"),
        IN_PROGRESS("진행중"),
        COMPLETED("완료"),
        CANCELLED("취소"),
        MISSED("미실시");
        
        private final String displayName;
        
        RecordStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
} 