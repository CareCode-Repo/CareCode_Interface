package com.carecode.core.client.sync;

import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.entity.FacilityCapacitySnapshot;
import com.carecode.domain.careFacility.repository.FacilityCapacitySnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** 동기화 시점의 정원·현원을 이력으로 남긴다. 시설 행은 최신값만 갖고, 추이는 여기에 쌓인다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CapacitySnapshotRecorder {

    private final FacilityCapacitySnapshotRepository snapshotRepository;

    /** 호출부의 트랜잭션에 참여한다. 스냅샷 실패가 시설 저장을 되돌리지 않도록 예외는 삼킨다. */
    public void record(CareFacility facility) {
        if (facility.getId() == null) {
            return;
        }
        // 정원·현원이 둘 다 없으면 추이 분석에 쓸 수 없으므로 남기지 않는다.
        if (facility.getCapacity() == null && facility.getCurrentEnrollment() == null) {
            return;
        }

        LocalDate today = LocalDate.now();
        try {
            snapshotRepository.findByFacilityIdAndObservedDate(facility.getId(), today)
                    .ifPresentOrElse(
                            existing -> existing.refresh(facility.getCapacity(),
                                    facility.getCurrentEnrollment(), facility.getAvailableSpots()),
                            () -> snapshotRepository.save(FacilityCapacitySnapshot.builder()
                                    .facilityId(facility.getId())
                                    .observedDate(today)
                                    .capacity(facility.getCapacity())
                                    .currentEnrollment(facility.getCurrentEnrollment())
                                    .availableSpots(facility.getAvailableSpots())
                                    .build()));
        } catch (Exception e) {
            log.warn("정원 스냅샷 기록 실패 - facilityId={}, 사유={}", facility.getId(), e.getMessage());
        }
    }
}
