package com.carecode.core.devtools;

import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.entity.FacilityCapacitySnapshot;
import com.carecode.domain.careFacility.repository.CareFacilityRepository;
import com.carecode.domain.careFacility.repository.FacilityCapacitySnapshotRepository;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 샘플 데이터 제거. 접두어로 식별하므로 실데이터는 건드리지 않는다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class SampleDataCleaner {

    private final PolicyRepository policyRepository;
    private final CareFacilityRepository facilityRepository;
    private final FacilityCapacitySnapshotRepository snapshotRepository;

    @Transactional
    public Map<String, Integer> clean() {
        List<Policy> policies = policyRepository.findAll().stream()
                .filter(p -> p.getPolicyCode() != null
                        && p.getPolicyCode().startsWith(SampleDataProperties.POLICY_PREFIX))
                .toList();
        policyRepository.deleteAll(policies);

        List<CareFacility> facilities = facilityRepository.findAll().stream()
                .filter(f -> f.getFacilityCode() != null
                        && f.getFacilityCode().startsWith(SampleDataProperties.FACILITY_PREFIX))
                .toList();

        // 스냅샷은 FK ON DELETE CASCADE 로 지워지지만, JPA 로 지울 때는 직접 정리해야 한다.
        int snapshots = 0;
        for (CareFacility facility : facilities) {
            List<FacilityCapacitySnapshot> owned =
                    snapshotRepository.findHistory(facility.getId(), LocalDate.EPOCH);
            snapshots += owned.size();
            snapshotRepository.deleteAll(owned);
        }
        facilityRepository.deleteAll(facilities);

        Map<String, Integer> removed = new LinkedHashMap<>();
        removed.put("policies", policies.size());
        removed.put("facilities", facilities.size());
        removed.put("snapshots", snapshots);
        log.warn("샘플 데이터를 제거했습니다 - {}", removed);
        return removed;
    }
}
