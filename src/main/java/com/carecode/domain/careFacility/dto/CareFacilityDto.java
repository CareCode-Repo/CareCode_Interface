package com.carecode.domain.careFacility.dto;

import com.carecode.domain.careFacility.entity.FacilityType;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * 육아 시설 정보 전송 객체
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareFacilityDto {
    
    @NotNull(message = "시설 ID는 필수입니다")
    private Long id;
    
    @NotBlank(message = "시설명은 필수입니다")
    @Size(max = 200, message = "시설명은 200자를 초과할 수 없습니다")
    private String name;
    
    @NotNull(message = "시설 유형은 필수입니다")
    private FacilityType facilityType;
    
    @Size(max = 2000, message = "시설 설명은 2000자를 초과할 수 없습니다")
    private String description;
    
    @NotBlank(message = "주소는 필수입니다")
    @Size(max = 500, message = "주소는 500자를 초과할 수 없습니다")
    private String address;
    
    @NotBlank(message = "지역은 필수입니다")
    @Size(max = 100, message = "지역명은 100자를 초과할 수 없습니다")
    private String location;
    
    @DecimalMin(value = "33.0", message = "위도는 33.0 이상이어야 합니다")
    @DecimalMax(value = "39.0", message = "위도는 39.0 이하여야 합니다")
    private Double latitude;
    
    @DecimalMin(value = "124.0", message = "경도는 124.0 이상이어야 합니다")
    @DecimalMax(value = "132.0", message = "경도는 132.0 이하여야 합니다")
    private Double longitude;
    
    @Pattern(regexp = "^[0-9-+\\s()]+$", message = "유효하지 않은 전화번호 형식입니다")
    @Size(max = 20, message = "전화번호는 20자를 초과할 수 없습니다")
    private String phoneNumber;
    
    @Email(message = "유효하지 않은 이메일 형식입니다")
    @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다")
    private String email;
    
    @Pattern(regexp = "^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})[/\\w .-]*/?$", 
             message = "유효하지 않은 웹사이트 URL입니다")
    private String websiteUrl;
    
    @Size(max = 200, message = "운영 시간은 200자를 초과할 수 없습니다")
    private String operatingHours;
    
    @Min(value = 1, message = "수용 인원은 1 이상이어야 합니다")
    @Max(value = 1000, message = "수용 인원은 1000을 초과할 수 없습니다")
    private Integer capacity;
    
    @Min(value = 0, message = "현재 등록 인원은 0 이상이어야 합니다")
    private Integer currentEnrollment;
    
    @Min(value = 0, message = "최소 연령은 0 이상이어야 합니다")
    @Max(value = 18, message = "최소 연령은 18 이하여야 합니다")
    private Integer minAge;
    
    @Min(value = 0, message = "최대 연령은 0 이상이어야 합니다")
    @Max(value = 18, message = "최대 연령은 18 이하여야 합니다")
    private Integer maxAge;
    
    @DecimalMin(value = "0.0", message = "평점은 0.0 이상이어야 합니다")
    @DecimalMax(value = "5.0", message = "평점은 5.0 이하여야 합니다")
    private Double rating;
    
    @Min(value = 0, message = "리뷰 수는 0 이상이어야 합니다")
    private Integer reviewCount;
    
    @Min(value = 0, message = "조회수는 0 이상이어야 합니다")
    private Integer viewCount;
    
    @NotNull(message = "활성화 상태는 필수입니다")
    private Boolean isActive;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
} 