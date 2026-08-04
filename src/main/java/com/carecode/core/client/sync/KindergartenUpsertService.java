package com.carecode.core.client.sync;

import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.entity.FacilityType;
import com.carecode.domain.careFacility.repository.CareFacilityRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.function.Consumer;

/** 유치원 한 건을 저장하는 트랜잭션 경계. */
@Slf4j
@Service
@RequiredArgsConstructor
public class KindergartenUpsertService {

    /** 어린이집 시설 코드와 충돌하지 않도록 구분한다. */
    public static final String CODE_PREFIX = "KG-";

    private final CareFacilityRepository careFacilityRepository;
    private final CapacitySnapshotRecorder snapshotRecorder;

    /** 시설 코드 기준 upsert. 표준데이터에 고유 코드가 없으면 이름+주소로 만들어 쓴다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean upsert(JsonNode row) {
        String name = text(row, "유치원명", "kindrgrtnNm", "KINDER_NM");
        String address = text(row, "소재지도로명주소", "rdnmadr", "소재지지번주소", "lnmadr", "ADDR");
        if (name == null) {
            throw new IllegalArgumentException("유치원명이 없는 응답입니다.");
        }

        String facilityCode = resolveCode(row, name, address);
        CareFacility facility = careFacilityRepository.findByFacilityCode(facilityCode).orElse(null);
        boolean isNew = facility == null;
        if (isNew) {
            facility = CareFacility.builder()
                    .facilityCode(facilityCode)
                    .facilityType(FacilityType.KINDERGARTEN)
                    .isActive(true)
                    .viewCount(0)
                    .build();
        }

        facility.setName(name);
        applyIfPresent(address, facility::setAddress);
        applyIfPresent(text(row, "전화번호", "telno", "TEL"), facility::setPhone);
        applyIfPresent(text(row, "홈페이지주소", "homepageAddr", "HOMEPAGE"), facility::setWebsite);
        applyIfPresent(text(row, "운영시간", "operPdCn"), facility::setOperatingHours);
        applyIfPresent(text(row, "시도명", "ctprvnNm"), facility::setCity);
        applyIfPresent(text(row, "시군구명", "signguNm"), facility::setDistrict);

        // 국공립 여부는 설립유형에서 판단한다. 사립은 비용 부담이 달라 사용자에게 중요한 구분이다.
        String establishment = text(row, "설립유형", "estblshSe", "설립구분");
        if (establishment != null) {
            facility.setIsPublic(establishment.contains("공립") || establishment.contains("국립"));
        }

        Integer capacity = integer(row, "정원", "fixnum", "TOTAL_CAPACITY");
        if (capacity != null) {
            facility.setCapacity(capacity);
        }
        Integer enrollment = integer(row, "현원", "nowNmpr", "CURRENT_CNT");
        if (enrollment != null) {
            facility.setCurrentEnrollment(enrollment);
            Integer effectiveCapacity = capacity != null ? capacity : facility.getCapacity();
            if (effectiveCapacity != null) {
                facility.setAvailableSpots(Math.max(0, effectiveCapacity - enrollment));
            }
        }

        Double lat = decimal(row, "위도", "latitude", "LAT");
        Double lng = decimal(row, "경도", "longitude", "LNG");
        if (lat != null && lng != null) {
            facility.setLatitude(lat);
            facility.setLongitude(lng);
        }

        facility.setUpdatedAt(LocalDateTime.now());
        careFacilityRepository.save(facility);
        snapshotRecorder.record(facility);
        return isNew;
    }

    /**
     * 표준데이터는 고유 식별자를 주지 않는 경우가 많다.
     * 이름+주소 해시를 코드로 쓰면 재동기화 때 같은 유치원이 중복 생성되지 않는다.
     */
    private String resolveCode(JsonNode row, String name, String address) {
        String external = text(row, "유치원코드", "kindrgrtnCode", "KINDER_CD");
        if (external != null) {
            return CODE_PREFIX + external;
        }
        String naturalKey = name + "|" + (address != null ? address : "");
        return CODE_PREFIX + hash(naturalKey);
    }

    private String hash(String value) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 12);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 을 사용할 수 없습니다.", e);
        }
    }

    private void applyIfPresent(String value, Consumer<String> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    /** 표준데이터는 한글 필드명, 오픈API 는 영문 필드명을 쓰므로 후보를 순서대로 본다. */
    private String text(JsonNode row, String... keys) {
        for (String key : keys) {
            JsonNode node = row.get(key);
            if (node != null && !node.isNull()) {
                String value = node.asText().trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }

    private Integer integer(JsonNode row, String... keys) {
        String value = text(row, keys);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value.replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double decimal(JsonNode row, String... keys) {
        String value = text(row, keys);
        if (value == null) {
            return null;
        }
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
