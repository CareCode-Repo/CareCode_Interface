package com.carecode.core.geocoding;

import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.repository.CareFacilityRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 좌표가 없는 시설의 주소를 좌표로 채운다.
 * 동기화 중에 인라인으로 돌리면 수집이 느려지고 외부 API 한도에 걸리므로 배치로 분리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FacilityGeocodingService {

    /** 한 번 실행에서 처리할 최대 건수. 외부 API 일일 한도를 넘지 않도록 나눠 돌린다. */
    @Value("${app.geocoding.batch-size:500}")
    private int batchSize;

    /** 호출 간 간격(ms). 초당 요청 제한에 걸리지 않게 한다. */
    @Value("${app.geocoding.delay-ms:100}")
    private long delayMs;

    private final CareFacilityRepository facilityRepository;
    private final Geocoder geocoder;

    @Getter
    public static class GeocodingResult {
        private int resolved;
        private int failed;
        private long remaining;
        private String skippedReason;

        @Override
        public String toString() {
            return skippedReason != null
                    ? "건너뜀 - " + skippedReason
                    : String.format("보정=%d, 실패=%d, 남은 대상=%d", resolved, failed, remaining);
        }
    }

    @Transactional
    public GeocodingResult fillMissingCoordinates() {
        GeocodingResult result = new GeocodingResult();

        if (!geocoder.isAvailable()) {
            result.skippedReason = "지오코딩 키 미설정";
            log.info("좌표 보정 건너뜀 - 키가 없습니다.");
            return result;
        }

        List<CareFacility> targets =
                facilityRepository.findMissingCoordinates(PageRequest.of(0, batchSize));
        if (targets.isEmpty()) {
            log.debug("좌표 보정 대상이 없습니다.");
            return result;
        }

        for (CareFacility facility : targets) {
            geocoder.geocode(facility.getAddress()).ifPresentOrElse(
                    coordinates -> {
                        facility.setLatitude(coordinates.latitude());
                        facility.setLongitude(coordinates.longitude());
                        facilityRepository.save(facility);
                        result.resolved++;
                    },
                    () -> result.failed++);

            pause();
        }

        result.remaining = facilityRepository.countMissingCoordinates();
        log.info("좌표 보정 완료 - {}", result);
        return result;
    }

    /** 외부 API 는 초당 요청 제한이 있다. 한 건씩 간격을 둔다. */
    private void pause() {
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
