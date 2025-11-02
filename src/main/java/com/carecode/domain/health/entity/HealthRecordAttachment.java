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

/*
 * @author CareCode Team
 * @since 1.0.0
 */
@Entity
@Table(name = "TBL_HEALTH_RECORD_ATTACHMENTS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class HealthRecordAttachment {

    /**
     * 첨부파일 고유 식별자
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    /**
     * 이 첨부파일이 속한 건강 기록
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "HEALTH_RECORD_ID", nullable = false)
    private HealthRecord healthRecord;

    /**
     * 파일 URL
     * 파일이 저장된 위치의 URL (최대 500자)
     */
    @Column(name = "FILE_URL", nullable = false, length = 500)
    private String fileUrl;

    /**
     * 파일명
     * 원본 파일명 (최대 200자)
     */
    @Column(name = "FILE_NAME", nullable = false, length = 200)
    private String fileName;

    /**
     * 파일 타입
     * MIME 타입 (예: image/jpeg, application/pdf) (최대 50자)
     */
    @Column(name = "FILE_TYPE", length = 50)
    private String fileType;

    /**
     * 파일 크기 (바이트)
     * 파일의 크기를 바이트 단위로 저장
     */
    @Column(name = "FILE_SIZE")
    private Long fileSize;

    /**
     * 첨부파일 설명
     * 첨부파일에 대한 설명 (최대 500자)
     */
    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    /**
     * 표시 순서
     * UI에서 첨부파일이 표시되는 순서 (기본값: 0)
     */
    @Column(name = "DISPLAY_ORDER", nullable = false)
    private Integer displayOrder = 0;

    /**
     * 활성 상태 여부
     * 첨부파일의 활성/비활성 상태 (기본값: true)
     */
    @Column(name = "IS_ACTIVE", nullable = false)
    private Boolean isActive = true;

    /**
     * 생성 일시
     * 첨부파일이 데이터베이스에 처음 저장된 시간 (JPA Auditing)
     */
    @CreatedDate
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정 일시
     * 첨부파일 정보가 마지막으로 수정된 시간 (JPA Auditing)
     */
    @LastModifiedDate
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    /**
     * 첨부파일 생성자
     * 
     * @param healthRecord 이 첨부파일이 속한 건강 기록
     * @param fileUrl 파일 URL
     * @param fileName 파일명
     * @param fileType 파일 타입
     * @param fileSize 파일 크기
     * @param description 첨부파일 설명
     * @param displayOrder 표시 순서
     */
    @Builder
    public HealthRecordAttachment(HealthRecord healthRecord, String fileUrl, String fileName,
                                String fileType, Long fileSize, String description, Integer displayOrder) {
        this.healthRecord = healthRecord;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    /**
     * 첨부파일 정보 업데이트
     * 
     * @param fileUrl 새로운 파일 URL
     * @param fileName 새로운 파일명
     * @param fileType 새로운 파일 타입
     * @param fileSize 새로운 파일 크기
     * @param description 새로운 첨부파일 설명
     * @param displayOrder 새로운 표시 순서
     */
    public void updateAttachment(String fileUrl, String fileName, String fileType,
                               Long fileSize, String description, Integer displayOrder) {
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    /**
     * 첨부파일 비활성화
     * 첨부파일을 비활성 상태로 변경
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 첨부파일 활성화
     * 비활성화된 첨부파일을 다시 활성 상태로 변경
     */
    public void activate() {
        this.isActive = true;
    }
} 