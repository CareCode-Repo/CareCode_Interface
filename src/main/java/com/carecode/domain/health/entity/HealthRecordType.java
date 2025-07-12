package com.carecode.domain.health.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 건강 기록 유형 엔티티
 * 건강 기록(예: 예방접종, 진료, 성장 등)의 유형을 정의하고 관리합니다.
 * @author CareCode Team
 * @since 1.0.0
 * @see HealthRecord
 */
@Entity
@Table(name = "health_record_types")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class HealthRecordType {

    /**
     * 건강 기록 유형 고유 식별자
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 건강 기록 유형명
     */
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    /**
     * 카테고리
     */
    @Column(name = "category", length = 50)
    private String category;

    /**
     * 유형 설명
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 표시 순서
     */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    /**
     * 활성 상태 여부
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * 생성 일시
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정 일시
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 이 유형에 속한 건강 기록 목록
     */
    @OneToMany(mappedBy = "healthRecordType", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HealthRecord> healthRecords = new ArrayList<>();

    /**
     * 건강 기록 유형 생성자
     *
     * @param name 유형명 (필수, 고유)
     * @param category 카테고리 (선택)
     * @param description 설명 (선택)
     * @param displayOrder 표시 순서 (기본값: 0)
     */
    @Builder
    public HealthRecordType(String name, String category, String description, Integer displayOrder) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    /**
     * 건강 기록 유형 정보 업데이트
     *
     * @param name 새로운 유형명
     * @param category 새로운 카테고리
     * @param description 새로운 설명
     * @param displayOrder 새로운 표시 순서
     */
    public void updateRecordType(String name, String category, String description, Integer displayOrder) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    /**
     * 유형 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 유형 활성화
     */
    public void activate() {
        this.isActive = true;
    }
} 