package com.carecode.core.client.sync;

import com.carecode.domain.health.entity.Hospital;
import com.carecode.domain.health.repository.HospitalRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 병원 한 건을 저장하는 트랜잭션 경계. */
@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalUpsertService {

    private final HospitalRepository hospitalRepository;

    /** 요양기호(ykiho) 기준 upsert. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean upsert(JsonNode row, String defaultType) {
        String ykiho = text(row, "ykiho", "YKIHO");
        if (ykiho == null) {
            throw new IllegalArgumentException("요양기호(ykiho)가 없는 응답입니다.");
        }

        Hospital hospital = hospitalRepository.findByExternalCode(ykiho).orElse(null);
        boolean isNew = hospital == null;
        if (isNew) {
            hospital = Hospital.builder().externalCode(ykiho).build();
        }

        String name = text(row, "yadmNm", "YADMNM");
        if (name != null) {
            hospital.setName(name);
        } else if (isNew) {
            // name 은 NOT NULL 이라 값이 없으면 저장할 수 없다.
            throw new IllegalArgumentException("요양기관명이 없는 응답입니다: ykiho=" + ykiho);
        }

        applyIfPresent(text(row, "addr", "ADDR"), hospital::setAddress);
        applyIfPresent(text(row, "telno", "TELNO"), hospital::setPhone);

        // clCdNm 은 "상급종합"·"의원" 같은 종별이다. 이걸 type 에 넣으면 소아과 검색이 안 된다.
        hospital.setType(defaultType);
        applyIfPresent(text(row, "clCdNm", "CLCDNM"), hospital::setGrade);

        // 심평원 좌표는 XPos=경도, YPos=위도 순서다. 뒤집으면 지도에서 엉뚱한 위치가 나온다.
        Double lng = decimal(row, "XPos", "XPOS");
        Double lat = decimal(row, "YPos", "YPOS");
        if (lat != null && lng != null) {
            hospital.setLatitude(lat);
            hospital.setLongitude(lng);
        }

        hospitalRepository.save(hospital);
        return isNew;
    }

    private void applyIfPresent(String value, java.util.function.Consumer<String> setter) {
        if (value != null) {
            setter.accept(value);
        }
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
