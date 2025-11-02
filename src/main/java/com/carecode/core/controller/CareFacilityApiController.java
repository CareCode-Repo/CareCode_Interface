package com.carecode.core.controller;

import com.carecode.core.client.CareFacilityApiService;
import com.carecode.domain.careFacility.dto.CareFacilityResponse;
import com.carecode.domain.careFacility.entity.FacilityType;
import com.carecode.domain.careFacility.service.CareFacilityService;
import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.repository.CareFacilityRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;
import java.util.Optional;

/**
 * 돌봄시설 공공데이터 API 컨트롤러
 * 보육시설 정보를 공공데이터 포털에서 가져와서 DB에 저장
 */
@Slf4j
@RestController
@RequestMapping("/api/public/care-facilities")
@RequiredArgsConstructor
@Tag(name = "돌봄시설 공공데이터", description = "공공데이터 포털의 돌봄시설 정보 API")
public class CareFacilityApiController {

    private final CareFacilityApiService careFacilityApiService;
    private final CareFacilityService careFacilityService;
    private final CareFacilityRepository careFacilityRepository;
    private FacilityType facilityType;

    /**
     * 전체 보육시설 정보 동기화 (모든 데이터)
     */
    @PostMapping("/sync-all")
    @Operation(summary = "전체 보육시설 정보 동기화", description = "공공데이터에서 모든 보육시설 정보를 조회하고 DB에 저장합니다.")
    public ResponseEntity<Map<String, Object>> syncAllChildcareFacilities(
            @Parameter(description = "시도명", example = "서울특별시") @RequestParam(defaultValue = "서울특별시") String sido,
            @Parameter(description = "시군구명", example = "강남구") @RequestParam(defaultValue = "강남구") String sigungu,
            @Parameter(description = "한 페이지 결과 수", example = "100") @RequestParam(defaultValue = "100") int numOfRows) {
        
        try {
            int totalSaved = 0;
            int totalUpdated = 0;
            int totalError = 0;
            int pageNo = 1;
            boolean hasMoreData = true;
            
            while (hasMoreData) {
                try {
                    log.info("페이지 {} 처리 중...", pageNo);
                    
                    // 1. 공공데이터 API에서 데이터 조회
                    Map<String, Object> publicData = careFacilityApiService.getChildcareFacilities(sido, sigungu, pageNo, numOfRows);
                    
                    // 2. 결과 확인
                    Boolean success = (Boolean) publicData.get("success");
                    if (success == null || !success) {
                        log.warn("페이지 {}에서 데이터 조회 실패: {}", pageNo, publicData.get("message"));
                        break;
                    }
                    
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> facilities = (List<Map<String, Object>>) publicData.get("facilities");
                    
                    if (facilities == null || facilities.isEmpty()) {
                        log.info("페이지 {}에 더 이상 데이터가 없습니다.", pageNo);
                        hasMoreData = false;
                        break;
                    }
                    
                    log.info("페이지 {}에서 {}개 시설 발견", pageNo, facilities.size());
                    
                    // 3. DB에 저장
                    int savedCount = 0;
                    int updatedCount = 0;
                    int errorCount = 0;
                    
                    for (Map<String, Object> facilityData : facilities) {
                        try {
                            String facilityId = (String) facilityData.get("facilityId");
                            
                            // 기존 시설이 있는지 확인
                            Optional<CareFacility> existingFacilityOpt = careFacilityRepository.findByFacilityCode(facilityId);
                            
                            if (existingFacilityOpt.isPresent()) {
                                // 기존 시설 업데이트
                                CareFacility existingFacility = existingFacilityOpt.get();
                                updateCareFacilityFromPublicData(existingFacility, facilityData);
                                updatedCount++;
                            } else {
                                // 새 시설 생성
                                CareFacility newFacility = createCareFacilityFromPublicData(facilityData);
                                careFacilityRepository.save(newFacility);
                                savedCount++;
                            }
                            
                        } catch (Exception e) {
                            errorCount++;
                            log.error("시설 데이터 저장 중 오류: {}", e.getMessage());
                        }
                    }
                    
                    totalSaved += savedCount;
                    totalUpdated += updatedCount;
                    totalError += errorCount;
                    
                    log.info("페이지 {} 완료: 신규={}, 업데이트={}, 오류={}", pageNo, savedCount, updatedCount, errorCount);
                    
                    // 다음 페이지로
                    pageNo++;
                    
                    // 너무 많은 페이지를 처리하지 않도록 제한 (안전장치)
                    if (pageNo > 50) {
                        log.warn("페이지 제한에 도달했습니다. (최대 50페이지)");
                        break;
                    }

                } catch (Exception e) {
                    log.error("페이지 {} 처리 중 오류: {}", pageNo, e.getMessage());
                    break;
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "전체 보육시설 정보 동기화 완료",
                "totalPages", pageNo - 1,
                "totalSaved", totalSaved,
                "totalUpdated", totalUpdated,
                "totalError", totalError
            ));
            
        } catch (Exception e) {
            log.error("전체 보육시설 정보 동기화 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "전체 보육시설 정보 동기화 실패: " + e.getMessage()
            ));
        }
    }

    /**
     * Swagger UI용 간단한 보육시설 동기화 (GET 방식)
     */
    @GetMapping("/swagger/sync")
    @Operation(summary = "Swagger용 보육시설 동기화", description = "Swagger UI에서 쉽게 테스트할 수 있는 GET 방식 동기화")
    public ResponseEntity<Map<String, Object>> swaggerSync() {
        try {
            log.info("Swagger용 보육시설 동기화 요청");

            // 기본 파라미터로 전체 동기화 실행
            return syncAllChildcareFacilities("서울특별시", "강남구", 1000);
            
        } catch (Exception e) {
            log.error("Swagger용 보육시설 동기화 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Swagger용 보육시설 동기화 실패: " + e.getMessage()
            ));
        }
    }

    /**
     * DB 저장된 보육시설 목록 조회
     */
    @GetMapping("/swagger/db-facilities")
    @Operation(summary = "DB 저장된 보육시설 목록", description = "DB에 저장된 보육시설 목록을 조회합니다")
    public ResponseEntity<Map<String, Object>> swaggerGetDbFacilities(
            @Parameter(description = "페이지 번호", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "한 페이지 결과 수", example = "10") @RequestParam(defaultValue = "10") int size) {
        
        try {
            log.info("DB 저장된 보육시설 목록 조회 요청: page={}, size={}", page, size);
            
            // CareFacilityService를 통해 DB에서 데이터 조회
            List<CareFacilityResponse.CareFacility> facilities = careFacilityService.getAllCareFacilities();
            
            // 페이징 처리
            int start = page * size;
            int end = Math.min(start + size, facilities.size());
            List<CareFacilityResponse.CareFacility> pagedFacilities = facilities.subList(start, end);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "DB 저장된 보육시설 목록 조회 성공",
                "totalCount", facilities.size(),
                "currentPage", page,
                "pageSize", size,
                "data", pagedFacilities
            ));
            
        } catch (Exception e) {
            log.error("DB 저장된 보육시설 목록 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "DB 저장된 보육시설 목록 조회 실패: " + e.getMessage()
            ));
        }
    }

    /**
     * 보육시설 통계
     */
    @GetMapping("/swagger/stats")
    @Operation(summary = "보육시설 통계", description = "DB에 저장된 보육시설 통계 정보를 조회합니다")
    public ResponseEntity<Map<String, Object>> swaggerGetStats() {
        try {
            log.info("보육시설 통계 조회 요청");
            
            // CareFacilityService를 통해 통계 조회
            CareFacilityResponse.CareFacilityStats stats = careFacilityService.getFacilityStats();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "보육시설 통계 조회 성공",
                "data", stats
            ));
            
        } catch (Exception e) {
            log.error("보육시설 통계 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "보육시설 통계 조회 실패: " + e.getMessage()
            ));
        }
    }

    /**
     * 공공데이터로부터 새로운 CareFacility 엔티티 생성
     */
    private CareFacility createCareFacilityFromPublicData(Map<String, Object> facilityData) {
        try {
            return CareFacility.builder()
                    .facilityCode((String) facilityData.get("facilityId"))
                    .name((String) facilityData.get("facilityName"))
                    .facilityType(mapServiceTypeToFacilityType((String) facilityData.get("serviceType")))
                    .city("서울특별시") // 서울시 API이므로 고정
                    .district((String) facilityData.get("district"))
                    .address((String) facilityData.get("address"))
                    .latitude(parseDouble(facilityData.get("latitude")))
                    .longitude(parseDouble(facilityData.get("longitude")))
                    .website((String) facilityData.get("websiteUrl"))
                    .ageRange((String) facilityData.get("ageGroup"))
                    .operatingHours(formatOperatingHours(facilityData))
                    .tuitionFee(parseInteger(facilityData.get("rentFee")))
                    .isActive(true)
                    .isPublic(true) // 공공데이터는 주로 공립 시설
                    .subsidyAvailable((Boolean) facilityData.get("isFree"))
                    .description(generateDescription(facilityData))
                    .build();
                    
        } catch (Exception e) {
            log.error("CareFacility 엔티티 생성 중 오류: {}", e.getMessage(), e);
            throw new RuntimeException("CareFacility 엔티티 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 기존 CareFacility 엔티티를 공공데이터로 업데이트
     */
    private void updateCareFacilityFromPublicData(CareFacility existingFacility, Map<String, Object> facilityData) {
        try {
            existingFacility.setName((String) facilityData.get("facilityName"));
            existingFacility.setFacilityType(mapServiceTypeToFacilityType((String) facilityData.get("serviceType")));
            existingFacility.setDistrict((String) facilityData.get("district"));
            existingFacility.setAddress((String) facilityData.get("address"));
            existingFacility.setLatitude(parseDouble(facilityData.get("latitude")));
            existingFacility.setLongitude(parseDouble(facilityData.get("longitude")));
            existingFacility.setWebsite((String) facilityData.get("websiteUrl"));
            existingFacility.setAgeRange((String) facilityData.get("ageGroup"));
            existingFacility.setOperatingHours(formatOperatingHours(facilityData));
            existingFacility.setTuitionFee(parseInteger(facilityData.get("rentFee")));
            existingFacility.setSubsidyAvailable((Boolean) facilityData.get("isFree"));
            existingFacility.setDescription(generateDescription(facilityData));
            
            careFacilityRepository.save(existingFacility);
            
        } catch (Exception e) {
            log.error("CareFacility 엔티티 업데이트 중 오류: {}", e.getMessage(), e);
            throw new RuntimeException("CareFacility 엔티티 업데이트 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 서비스 타입을 FacilityType으로 매핑
     */
    private FacilityType mapServiceTypeToFacilityType(String serviceType) {
        if (serviceType == null) {
            return FacilityType.OTHER;
        }
        
        switch (serviceType) {
            case "공동육아나눔터":
                return FacilityType.PLAYGROUP;
            case "어린이집":
                return FacilityType.DAYCARE;
            case "유치원":
                return FacilityType.KINDERGARTEN;
            case "방과후교실":
                return FacilityType.NURSERY;
            default:
                return FacilityType.OTHER;
        }
    }

    /**
     * 운영시간 정보 포맷팅
     */
    private String formatOperatingHours(Map<String, Object> facilityData) {
        StringBuilder hours = new StringBuilder();
        
        String weekdayStart = (String) facilityData.get("weekdayStartTime");
        String weekdayEnd = (String) facilityData.get("weekdayEndTime");
        String operationType = (String) facilityData.get("operationType");
        Boolean hasSaturday = (Boolean) facilityData.get("hasSaturdayOperation");
        String saturdayStart = (String) facilityData.get("saturdayStartTime");
        String saturdayEnd = (String) facilityData.get("saturdayEndTime");
        
        if (weekdayStart != null && weekdayEnd != null) {
            hours.append("평일: ").append(weekdayStart).append("~").append(weekdayEnd);
        }
        
        if (operationType != null && !operationType.isEmpty()) {
            hours.append(" (").append(operationType).append(")");
        }
        
        if (hasSaturday != null && hasSaturday && saturdayStart != null && saturdayEnd != null) {
            hours.append(", 토요일: ").append(saturdayStart).append("~").append(saturdayEnd);
        }
        
        return hours.toString();
    }

    /**
     * 시설 설명 생성
     */
    private String generateDescription(Map<String, Object> facilityData) {
        StringBuilder description = new StringBuilder();
        
        String serviceType = (String) facilityData.get("serviceType");
        String ageGroup = (String) facilityData.get("ageGroup");
        String district = (String) facilityData.get("district");
        Boolean isFree = (Boolean) facilityData.get("isFree");
        
        if (serviceType != null) {
            description.append(serviceType);
        }
        
        if (ageGroup != null) {
            description.append(" - ").append(ageGroup);
        }
        
        if (district != null) {
            description.append(" (").append(district).append(")");
        }
        
        if (isFree != null && isFree) {
            description.append(" - 무료 이용 가능");
        }
        
        return description.toString();
    }

    /**
     * Double 파싱 헬퍼 메서드
     */
    private Double parseDouble(Object value) {
        if (value == null || value.toString().trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Double 파싱 실패: {}", value);
            return null;
        }
    }

    /**
     * Integer 파싱 헬퍼 메서드
     */
    private Integer parseInteger(Object value) {
        if (value == null || value.toString().trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Integer 파싱 실패: {}", value);
            return null;
        }
    }
}
