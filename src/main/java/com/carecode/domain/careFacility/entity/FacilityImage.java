package com.carecode.domain.careFacility.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 보육시설 이미지 엔티티
 * Next.js Image 태그를 위한 alt 속성 포함
 */
@Entity
@Table(name = "facility_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class FacilityImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private CareFacility careFacility;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl; // Next.js Image src

    @Column(name = "alt_text", nullable = false, length = 200)
    private String altText; // Next.js Image alt

    @Column(name = "title", length = 100)
    private String title; // 이미지 제목

    @Column(name = "description", length = 500)
    private String description; // 이미지 설명

    @Column(name = "image_type", length = 50)
    private String imageType; // EXTERIOR, INTERIOR, ACTIVITY, MENU 등

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_main", nullable = false)
    private Boolean isMain = false; // 대표 이미지 여부

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public FacilityImage(CareFacility careFacility, String imageUrl, String altText,
                        String title, String description, String imageType, 
                        Integer displayOrder, Boolean isMain) {
        this.careFacility = careFacility;
        this.imageUrl = imageUrl;
        this.altText = altText;
        this.title = title;
        this.description = description;
        this.imageType = imageType;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.isMain = isMain != null ? isMain : false;
    }

    public void updateImage(String imageUrl, String altText, String title,
                          String description, String imageType, Integer displayOrder) {
        this.imageUrl = imageUrl;
        this.altText = altText;
        this.title = title;
        this.description = description;
        this.imageType = imageType;
        this.displayOrder = displayOrder != null ? displayOrder : this.displayOrder;
    }

    public void setAsMain() {
        this.isMain = true;
    }

    public void setAsNotMain() {
        this.isMain = false;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }
} 