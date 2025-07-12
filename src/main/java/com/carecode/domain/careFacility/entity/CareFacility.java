package com.carecode.domain.careFacility.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 육아 시설 엔티티
 * 어린이집, 유치원 등 육아 관련 시설 정보를 관리
 */
@Entity
@Table(name = "care_facilities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareFacility {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "facility_code", nullable = false, unique = true)
    private String facilityCode;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "facility_type")
    private String facilityType; // 어린이집, 유치원, 놀이방 등
    
    @Column(name = "address")
    private String address;
    
    @Column(name = "latitude")
    private Double latitude;
    
    @Column(name = "longitude")
    private Double longitude;
    
    @Column(name = "phone")
    private String phone;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "website")
    private String website;
    
    @Column(name = "capacity")
    private Integer capacity;
    
    @Column(name = "current_enrollment")
    private Integer currentEnrollment;
    
    @Column(name = "available_spots")
    private Integer availableSpots;
    
    @Column(name = "age_range_min")
    private Integer ageRangeMin;
    
    @Column(name = "age_range_max")
    private Integer ageRangeMax;
    
    @Column(name = "operating_hours")
    private String operatingHours;
    
    @Column(name = "tuition_fee")
    private Integer tuitionFee;
    
    @Column(name = "additional_fees")
    private String additionalFees;
    
    @Column(name = "facilities")
    private String facilities; // 시설 내 편의시설
    
    @Column(name = "curriculum")
    private String curriculum;
    
    @Column(name = "teacher_count")
    private Integer teacherCount;
    
    @Column(name = "student_teacher_ratio")
    private String studentTeacherRatio;
    
    @Column(name = "accreditation")
    private String accreditation; // 인증 정보
    
    @Column(name = "rating")
    private Double rating;
    
    @Column(name = "review_count")
    private Integer reviewCount;
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    @Column(name = "is_public")
    private Boolean isPublic; // 공립/사립 구분
    
    @Column(name = "subsidy_available")
    private Boolean subsidyAvailable; // 보조금 지원 여부
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "careFacility", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 