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
}
