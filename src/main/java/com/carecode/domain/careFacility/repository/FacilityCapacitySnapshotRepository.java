package com.carecode.domain.careFacility.repository;

import com.carecode.domain.careFacility.entity.FacilityCapacitySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FacilityCapacitySnapshotRepository extends JpaRepository<FacilityCapacitySnapshot, Long> {

    Optional<FacilityCapacitySnapshot> findByFacilityIdAndObservedDate(Long facilityId, LocalDate observedDate);

    /** 예측 입력. 오래된 것부터 줘야 증감을 순서대로 훑을 수 있다. */
    @Query("SELECT s FROM FacilityCapacitySnapshot s "
            + "WHERE s.facilityId = :facilityId AND s.observedDate >= :from "
            + "ORDER BY s.observedDate ASC")
    List<FacilityCapacitySnapshot> findHistory(@Param("facilityId") Long facilityId,
                                               @Param("from") LocalDate from);

    /** 관측 기간이 얼마나 쌓였는지. 예측 가능 여부 판단에 쓴다. */
    @Query("SELECT MIN(s.observedDate) FROM FacilityCapacitySnapshot s WHERE s.facilityId = :facilityId")
    Optional<LocalDate> findEarliestObservedDate(@Param("facilityId") Long facilityId);

    long countByFacilityId(Long facilityId);
}
