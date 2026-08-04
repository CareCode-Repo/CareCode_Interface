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

import java.time.LocalDateTime;

/** 시설 한 건을 저장하는 트랜잭션 경계. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CareFacilityUpsertService {

    private final CareFacilityRepository careFacilityRepository;

    /** 시설 코드 기준 upsert. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean upsert(JsonNode row) {
        String facilityCode = text(row, "STCODE", "crcodeCd", "crcode");
        if (facilityCode == null) {
            throw new IllegalArgumentException("시설 코드가 없는 응답입니다.");
        }

        CareFacility facility = careFacilityRepository.findByFacilityCode(facilityCode).orElse(null);
        boolean isNew = facility == null;
        if (isNew) {
            facility = CareFacility.builder()
                    .facilityCode(facilityCode)
                    .isActive(true)
                    .viewCount(0)
                    .build();
        }

        applyIfPresent(text(row, "CRNAME", "crname"), facility::setName);
        applyIfPresent(text(row, "CRADDR", "craddr"), facility::setAddress);
        applyIfPresent(text(row, "CRTELNO", "crtelno"), facility::setPhone);
        applyIfPresent(text(row, "CRHOME", "crhome"), facility::setWebsite);

        String typeName = text(row, "CRTYPENAME", "crtypeName");
        if (typeName != null) {
            facility.setFacilityType(resolveType(typeName));
        } else if (isNew) {
            facility.setFacilityType(FacilityType.DAYCARE);
        }

        Integer capacity = integer(row, "CRCAPAT", "crcapat");
        if (capacity != null) {
            facility.setCapacity(capacity);
        }
        Integer enrollment = integer(row, "CRCHCNT", "crchcnt");
        if (enrollment != null) {
            facility.setCurrentEnrollment(enrollment);
            Integer effectiveCapacity = capacity != null ? capacity : facility.getCapacity();
            if (effectiveCapacity != null) {
                facility.setAvailableSpots(Math.max(0, effectiveCapacity - enrollment));
            }
        }

        Double lat = decimal(row, "LA", "la", "LAT");
        Double lng = decimal(row, "LO", "lo", "LNG");
        if (lat != null && lng != null) {
            facility.setLatitude(lat);
            facility.setLongitude(lng);
        }

        facility.setUpdatedAt(LocalDateTime.now());
        careFacilityRepository.save(facility);
        return isNew;
    }

    private void applyIfPresent(String value, java.util.function.Consumer<String> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    /** 응답 필드명이 대문자/카멜케이스로 섞여 오는 경우가 있어 후보를 순서대로 본다. */
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

    private FacilityType resolveType(String typeName) {
        if (typeName.contains("유치원")) {
            return FacilityType.KINDERGARTEN;
        }
        if (typeName.contains("어린이집")) {
            return FacilityType.DAYCARE;
        }
        return FacilityType.OTHER;
    }
}
