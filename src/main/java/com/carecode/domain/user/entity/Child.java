package com.carecode.domain.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 자녀 엔티티
 * 사용자의 자녀 정보를 관리
 */
@Entity
@Table(name = "TBL_CHILD")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Child {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;
    
    @Column(name = "NAME", nullable = false)
    private String name;
    
    @Column(name = "AGE")
    private Integer age;
    
    @Column(name = "GENDER")
    private String gender;
    
    @Column(name = "BIRTH_DATE")
    private LocalDate birthDate;
    
    @Column(name = "SPECIAL_NEEDS")
    private String specialNeeds;
    
    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // 비즈니스 로직 메서드들
    public int getAge() {
        if (birthDate != null) {
            return LocalDate.now().getYear() - birthDate.getYear();
        }
        return age != null ? age : 0;
    }
    
    public boolean isInAgeRange(int minAge, int maxAge) {
        int currentAge = getAge();
        return currentAge >= minAge && currentAge <= maxAge;
    }
    
    public boolean hasSpecialNeeds() {
        return specialNeeds != null && !specialNeeds.trim().isEmpty();
    }
    
    public String getDisplayName() {
        return name != null ? name : "자녀";
    }
} 