package com.carecode.core.devtools;

import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.entity.FacilityCapacitySnapshot;
import com.carecode.domain.careFacility.entity.FacilityType;
import com.carecode.domain.careFacility.repository.CareFacilityRepository;
import com.carecode.domain.careFacility.repository.FacilityCapacitySnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 입소 예측·인기도 분석을 확인하기 위한 샘플 시설과 정원 관측 이력.
 * 각 시설이 서로 다른 충원 패턴을 갖도록 해서 분석 결과가 갈리는지 볼 수 있게 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SampleFacilitySeeder {

    /** 월 1회 관측을 이만큼 만든다. 신학기 사이클이 한 번 이상 들어가야 의미가 있다. */
    private static final int MONTHS_OF_HISTORY = 18;
    private static final int CAPACITY = 100;

    private final CareFacilityRepository facilityRepository;
    private final FacilityCapacitySnapshotRepository snapshotRepository;

    @Transactional
    public int seed() {
        int created = 0;
        for (Pattern pattern : Pattern.values()) {
            String code = SampleDataProperties.FACILITY_PREFIX + pattern.name();
            if (facilityRepository.findByFacilityCode(code).isPresent()) {
                continue;
            }
            CareFacility facility = facilityRepository.save(buildFacility(code, pattern));
            seedSnapshots(facility, pattern);
            created++;
        }
        return created;
    }

    /** 분석 결과가 서로 다르게 나와야 기능을 확인할 수 있다. */
    private enum Pattern {
        /** 정원이 늘 차 있음 → IN_DEMAND, 입소 확률 낮음 */
        ALWAYS_FULL("늘찬어린이집", "고흥군"),
        /** 정원 여유 지속 → UNDERSUBSCRIBED, 입소 확률 높음 */
        UNDERSUBSCRIBED("여유어린이집", "고흥군"),
        /** 충원율 하락 추세 → FALLING */
        DECLINING("내리막어린이집", "성남시"),
        /** 3월이 아닌 시점에 급락 → 운영 변화 신호 */
        SHARP_DROP("변동어린이집", "성남시");

        final String name;
        final String region;

        Pattern(String name, String region) {
            this.name = name;
            this.region = region;
        }
    }

    private CareFacility buildFacility(String code, Pattern pattern) {
        return CareFacility.builder()
                .facilityCode(code)
                .name("[샘플] " + pattern.name)
                .facilityType(FacilityType.DAYCARE)
                .address(pattern.region + " 샘플로 1")
                .city(pattern.region)
                .latitude(37.5 + Math.random() * 0.01)
                .longitude(127.0 + Math.random() * 0.01)
                .capacity(CAPACITY)
                .isActive(true)
                .isPublic(true)
                .viewCount(0)
                .build();
    }

    /** 과거 MONTHS_OF_HISTORY 개월간 월 1회 관측을 만든다. */
    private void seedSnapshots(CareFacility facility, Pattern pattern) {
        LocalDate start = LocalDate.now().minusMonths(MONTHS_OF_HISTORY);

        for (int i = 0; i < MONTHS_OF_HISTORY; i++) {
            LocalDate observedDate = start.plusMonths(i);
            int enrolled = enrollmentFor(pattern, i, observedDate);

            snapshotRepository.save(FacilityCapacitySnapshot.builder()
                    .facilityId(facility.getId())
                    .observedDate(observedDate)
                    .capacity(CAPACITY)
                    .currentEnrollment(enrolled)
                    .availableSpots(Math.max(0, CAPACITY - enrolled))
                    .build());
        }

        // 시설 행에는 최신 관측값을 반영해 둔다.
        int latest = enrollmentFor(pattern, MONTHS_OF_HISTORY - 1, start.plusMonths(MONTHS_OF_HISTORY - 1L));
        facility.setCurrentEnrollment(latest);
        facility.setAvailableSpots(Math.max(0, CAPACITY - latest));
        facilityRepository.save(facility);
    }

    private int enrollmentFor(Pattern pattern, int monthIndex, LocalDate date) {
        // 3월 신학기에는 졸업·승급으로 어느 시설이든 일시적으로 자리가 난다.
        boolean newTerm = date.getMonthValue() == 3;

        return switch (pattern) {
            case ALWAYS_FULL -> newTerm ? 92 : 100;
            case UNDERSUBSCRIBED -> newTerm ? 45 : 55 + (monthIndex % 3);
            // 95 에서 시작해 서서히 내려간다
            case DECLINING -> Math.max(50, 95 - monthIndex * 3);
            // 중간 지점에서 한 번 크게 떨어진 뒤 회복하지 않는다
            case SHARP_DROP -> monthIndex < MONTHS_OF_HISTORY / 2 ? 95 : 62;
        };
    }
}
