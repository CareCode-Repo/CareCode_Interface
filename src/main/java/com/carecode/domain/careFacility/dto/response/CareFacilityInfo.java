package com.carecode.domain.careFacility.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 보육시설 정보 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareFacilityInfo {
    private Long id;
    private String name;
    private String facilityType;
    private String address;
    private String phoneNumber;
    private String email;
    private Double latitude;
    private Double longitude;
    private String description;
    private String operatingHours;
    private String website;
    private Double rating;
    private Long reviewCount;
    private Long likeCount;
    private Boolean isLiked;
    private String imageUrl;
    private List<String> amenities;
    private Map<String, Object> additionalInfo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

