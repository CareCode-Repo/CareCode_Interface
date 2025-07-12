package com.carecode.domain.policy.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "TBL_POLICY_DOCUMENTS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PolicyDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(name = "document_url", nullable = false, length = 500)
    private String documentUrl;

    @Column(name = "document_type", length = 50)
    private String documentType;

    @Column(name = "file_name", nullable = false, length = 200)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public PolicyDocument(Policy policy, String documentUrl, String documentType,
                         String fileName, Long fileSize, String description, Integer displayOrder) {
        this.policy = policy;
        this.documentUrl = documentUrl;
        this.documentType = documentType;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    public void updateDocument(String documentUrl, String documentType, String fileName,
                             Long fileSize, String description, Integer displayOrder) {
        this.documentUrl = documentUrl;
        this.documentType = documentType;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }
} 