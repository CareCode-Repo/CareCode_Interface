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
import java.util.function.Consumer;

/** 유치원 한 건을 저장하는 트랜잭션 경계. 필드명은 유치원알리미 basicInfo2 응답 기준이다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class KindergartenUpsertService {

    /** 어린이집 시설 코드와 충돌하지 않도록 구분한다. */
    public static final String CODE_PREFIX = "KG-";

    private final CareFacilityRepository careFacilityRepository;
    private final CapacitySnapshotRecorder snapshotRecorder;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean upsert(JsonNode row) {
        String externalCode = text(row, "kindercode");
        String name = text(row, "kindername");
        if (externalCode == null || name == null) {
            throw new IllegalArgumentException("유치원 코드 또는 이름이 없는 응답입니다.");
        }

        String facilityCode = CODE_PREFIX + externalCode;
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
        String address = text(row, "addr");
        applyIfPresent(address, facility::setAddress);
        applyIfPresent(text(row, "telno"), facility::setPhone);
        applyIfPresent(text(row, "hpaddr"), facility::setWebsite);
        applyIfPresent(text(row, "opertime"), facility::setOperatingHours);
        applyRegion(facility, address);

        // "공립(병설)" / "사립(사인)" 형태로 온다. 비용 부담이 달라 사용자에게 중요한 구분이다.
        String establish = text(row, "establish");
        if (establish != null) {
            facility.setIsPublic(establish.contains("공립") || establish.contains("국립"));
        }

        applyCapacity(facility, row);

        Double lat = decimal(row, "lttdcdnt");
        Double lng = decimal(row, "lngtcdnt");
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
     * 정원은 연령별 편성정원의 합을 쓴다.
     * prmstfcnt(인가정원)는 상한이라 늘 여유 있어 보여 충원율 분모로는 실제와 어긋난다.
     */
    private void applyCapacity(CareFacility facility, JsonNode row) {
        Integer classCapacity = sum(row, "ag3fpcnt", "ag4fpcnt", "ag5fpcnt", "mixfpcnt", "spcnfpcnt");
        Integer capacity = classCapacity != null && classCapacity > 0
                ? classCapacity
                : integer(row, "prmstfcnt");
        if (capacity != null) {
            facility.setCapacity(capacity);
        }

        Integer enrolled = sum(row, "ppcnt3", "ppcnt4", "ppcnt5", "mixppcnt", "shppcnt");
        if (enrolled != null) {
            facility.setCurrentEnrollment(enrolled);
            Integer effective = capacity != null ? capacity : facility.getCapacity();
            if (effective != null) {
                facility.setAvailableSpots(Math.max(0, effective - enrolled));
            }
        }
    }

    /** 주소 앞부분이 시도·시군구다. 지역 필터에 쓰이므로 분리해 둔다. */
    private void applyRegion(CareFacility facility, String address) {
        if (address == null || address.isBlank()) {
            return;
        }
        String[] parts = address.trim().split("\\s+");
        facility.setCity(parts[0]);
        if (parts.length >= 2) {
            facility.setDistrict(parts[1]);
        }
    }

    private void applyIfPresent(String value, Consumer<String> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    /** 하나라도 값이 있으면 합계를 낸다. 전부 없으면 null 이라 기존 값을 덮어쓰지 않는다. */
    private Integer sum(JsonNode row, String... keys) {
        int total = 0;
        boolean any = false;
        for (String key : keys) {
            Integer value = integer(row, key);
            if (value != null) {
                total += value;
                any = true;
            }
        }
        return any ? total : null;
    }

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
