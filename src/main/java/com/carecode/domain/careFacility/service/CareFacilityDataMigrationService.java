package com.carecode.domain.careFacility.service;

import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.repository.CareFacilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * CareFacility 데이터 마이그레이션 서비스
 * Hospital 데이터를 CareFacility로 변환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CareFacilityDataMigrationService implements CommandLineRunner {

    private final CareFacilityRepository careFacilityRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        migrateHospitalDataToCareFacility();
    }

    /**
     * Hospital 데이터를 CareFacility로 마이그레이션
     */
    private void migrateHospitalDataToCareFacility() {
        try {
            // 이미 CareFacility 데이터가 있으면 마이그레이션 건너뛰기
            if (careFacilityRepository.count() > 0) {
                log.info("CareFacility 데이터가 이미 존재합니다. 마이그레이션을 건너뜁니다.");
                return;
            }

            // Hospital 데이터 조회
            String sql = "SELECT * FROM tbl_hospital";
            List<Map<String, Object>> hospitals = jdbcTemplate.queryForList(sql);

            if (hospitals.isEmpty()) {
                log.warn("Hospital 데이터가 없습니다.");
                return;
            }

            log.info("Hospital 데이터 {}개를 CareFacility로 마이그레이션 시작", hospitals.size());

            for (Map<String, Object> hospital : hospitals) {
                CareFacility careFacility = createCareFacilityFromHospital(hospital);
                careFacilityRepository.save(careFacility);
            }

            log.info("Hospital 데이터 마이그레이션 완료: {}개", hospitals.size());

        } catch (Exception e) {
            log.error("Hospital 데이터 마이그레이션 중 오류 발생: {}", e.getMessage());
        }
    }

    /**
     * Hospital 데이터로부터 CareFacility 생성
     */
    private CareFacility createCareFacilityFromHospital(Map<String, Object> hospital) {
        String name = (String) hospital.get("name");
        String address = (String) hospital.get("address");
        String phone = (String) hospital.get("phone");
        Double latitude = (Double) hospital.get("latitude");
        Double longitude = (Double) hospital.get("longitude");

        // 주소에서 시/구 추출
        String city = extractCity(address);
        String district = extractDistrict(address);

        return CareFacility.builder()
                .facilityCode("CF" + System.currentTimeMillis() + (int)(Math.random() * 1000))
                .name(name)
                .facilityType("HOSPITAL") // 병원/의원으로 설정
                .city(city)
                .district(district)
                .address(address)
                .latitude(latitude)
                .longitude(longitude)
                .phone(phone)
                .capacity(50) // 기본값
                .currentEnrollment(0)
                .availableSpots(50)
                .ageRangeMin(0)
                .ageRangeMax(18)
                .operatingHours("09:00-18:00")
                .tuitionFee(0) // 병원은 수업료 없음
                .rating(4.5) // 기본 평점
                .reviewCount(0)
                .viewCount(0)
                .description(name + "에서 아이들의 건강을 관리합니다.")
                .ageRange("0-18세")
                .isActive(true)
                .isPublic(false)
                .subsidyAvailable(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 주소에서 시 추출
     */
    private String extractCity(String address) {
        if (address == null) return "서울시";
        
        if (address.contains("서울시")) return "서울시";
        if (address.contains("경북")) return "경북";
        if (address.contains("부산시")) return "부산시";
        if (address.contains("대구시")) return "대구시";
        if (address.contains("인천시")) return "인천시";
        if (address.contains("광주시")) return "광주시";
        if (address.contains("대전시")) return "대전시";
        if (address.contains("울산시")) return "울산시";
        
        return "서울시"; // 기본값
    }

    /**
     * 주소에서 구 추출
     */
    private String extractDistrict(String address) {
        if (address == null) return "강남구";
        
        if (address.contains("강남구")) return "강남구";
        if (address.contains("서초구")) return "서초구";
        if (address.contains("송파구")) return "송파구";
        if (address.contains("마포구")) return "마포구";
        if (address.contains("북구")) return "북구";
        if (address.contains("중구")) return "중구";
        if (address.contains("영등포구")) return "영등포구";
        if (address.contains("강서구")) return "강서구";
        if (address.contains("강동구")) return "강동구";
        if (address.contains("성동구")) return "성동구";
        if (address.contains("광진구")) return "광진구";
        if (address.contains("용산구")) return "용산구";
        if (address.contains("서대문구")) return "서대문구";
        if (address.contains("은평구")) return "은평구";
        if (address.contains("노원구")) return "노원구";
        if (address.contains("도봉구")) return "도봉구";
        if (address.contains("강북구")) return "강북구";
        if (address.contains("종로구")) return "종로구";
        if (address.contains("중랑구")) return "중랑구";
        
        return "강남구"; // 기본값
    }
} 