package com.carecode.domain.health.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "TBL_HOSPITAL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hospital {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 심평원 암호화 요양기호(ykiho).
     * 공공데이터 동기화 시 중복 적재를 막는 외부 식별자다.
     * 복호화 수단은 제공되지 않으므로 값 자체를 키로 쓴다.
     */
    @Column(name = "external_code", unique = true, length = 100)
    private String externalCode;

    @Column(nullable = false)
    private String name;

    @Column
    private String type; // 소아과, 산부인과 등

    @Column
    private String address;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column
    private String phone;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
} 