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

    /** 심평원 암호화 요양기호(ykiho). */
    @Column(name = "external_code", unique = true, length = 100)
    private String externalCode;

    @Column(nullable = false)
    private String name;

    @Column
    private String type; // 진료과목 (소아청소년과 등)

    /** 요양기관 종별. 동네 의원과 대학병원은 부모의 선택 기준이 다르다. */
    @Column(name = "GRADE")
    private String grade;

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