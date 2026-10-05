package com.carecode.domain.facility.repository;

import com.carecode.domain.facility.entity.FacilityCapacitySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
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

    /**
     * 여러 시설의 관측을 한 번에 읽는다.
     *
     * <p>지역 안의 시설마다 {@link #findHistory} 를 부르면 후보 수만큼 질의가 나간다. 한 구에 어린이집이
     * 수백 곳이라 그대로 두면 추천 한 번에 수백 번 왕복한다. 시설 순서를 함께 정렬해 호출부가 한 번만
     * 훑어도 시설별로 나눌 수 있게 한다.
     */
    @Query("SELECT s FROM FacilityCapacitySnapshot s "
            + "WHERE s.facilityId IN :facilityIds AND s.observedDate >= :from "
            + "ORDER BY s.facilityId ASC, s.observedDate ASC")
    List<FacilityCapacitySnapshot> findHistories(@Param("facilityIds") Collection<Long> facilityIds,
                                                 @Param("from") LocalDate from);

    /** 관측 기간이 얼마나 쌓였는지. 예측 가능 여부 판단에 쓴다. */
    @Query("SELECT MIN(s.observedDate) FROM FacilityCapacitySnapshot s WHERE s.facilityId = :facilityId")
    Optional<LocalDate> findEarliestObservedDate(@Param("facilityId") Long facilityId);

    long countByFacilityId(Long facilityId);

    /** 백테스트 대상. 관측이 몇 번 이상 쌓인 시설만 검증할 수 있다. */
    @Query("SELECT s.facilityId FROM FacilityCapacitySnapshot s "
            + "GROUP BY s.facilityId HAVING COUNT(s) >= :minSnapshots")
    List<Long> findFacilityIdsWithAtLeast(@Param("minSnapshots") long minSnapshots);
}
