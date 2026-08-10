package com.carecode.domain.careFacility.repository;

import com.carecode.domain.careFacility.entity.FacilityWaitlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacilityWaitlistRepository extends JpaRepository<FacilityWaitlist, Long> {

    Optional<FacilityWaitlist> findByFacilityIdAndChildId(Long facilityId, Long childId);

    List<FacilityWaitlist> findByUserIdOrderByAppliedAtDesc(Long userId);

    /** 입소까지 간 기록만. 대기 기간 통계의 표본이 된다. */
    @Query("SELECT w FROM FacilityWaitlist w "
            + "WHERE w.facilityId = :facilityId "
            + "AND w.status = com.carecode.domain.careFacility.entity.FacilityWaitlist.WaitStatus.ADMITTED "
            + "AND w.resolvedAt IS NOT NULL")
    List<FacilityWaitlist> findAdmitted(@Param("facilityId") Long facilityId);

    @Query("SELECT COUNT(w) FROM FacilityWaitlist w WHERE w.facilityId = :facilityId "
            + "AND w.status = com.carecode.domain.careFacility.entity.FacilityWaitlist.WaitStatus.WAITING")
    long countWaiting(@Param("facilityId") Long facilityId);

    /**
     * 대기자가 남아 있는 시설만.
     *
     * <p>빈자리를 확인할 대상을 여기서 좁힌다. 전국 시설을 다 뒤지면 대부분이 아무도
     * 기다리지 않는 곳이라 헛일이다.
     */
    @Query("SELECT DISTINCT w.facilityId FROM FacilityWaitlist w "
            + "WHERE w.status = com.carecode.domain.careFacility.entity.FacilityWaitlist.WaitStatus.WAITING")
    List<Long> findFacilityIdsWithWaiting();

    /** 해당 시설에서 아직 기다리는 사람들. 알림 대상이다. */
    @Query("SELECT w FROM FacilityWaitlist w "
            + "WHERE w.facilityId = :facilityId "
            + "AND w.status = com.carecode.domain.careFacility.entity.FacilityWaitlist.WaitStatus.WAITING "
            + "ORDER BY w.appliedAt ASC")
    List<FacilityWaitlist> findWaiting(@Param("facilityId") Long facilityId);
}
