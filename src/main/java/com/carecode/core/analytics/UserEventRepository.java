package com.carecode.core.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface UserEventRepository extends JpaRepository<UserEvent, Long> {

    /** 퍼널 단계별 고유 사용자 수. 이벤트 발생 횟수가 아니라 사람 수를 센다. */
    @Query("SELECT COUNT(DISTINCT e.userId) FROM UserEvent e "
            + "WHERE e.eventType = :type AND e.occurredDate BETWEEN :from AND :to")
    long countDistinctUsers(@Param("type") EventType type,
                            @Param("from") LocalDate from,
                            @Param("to") LocalDate to);

    /** 앞 단계를 거친 사용자 중 뒤 단계까지 간 사람 수. */
    @Query("SELECT COUNT(DISTINCT e2.userId) FROM UserEvent e2 "
            + "WHERE e2.eventType = :next AND e2.occurredDate BETWEEN :from AND :to "
            + "AND e2.userId IN (SELECT e1.userId FROM UserEvent e1 "
            + "WHERE e1.eventType = :previous AND e1.occurredDate BETWEEN :from AND :to)")
    long countConverted(@Param("previous") EventType previous,
                        @Param("next") EventType next,
                        @Param("from") LocalDate from,
                        @Param("to") LocalDate to);

    /** 가입일이 기준일인 사용자들. 리텐션의 분모가 된다. */
    @Query("SELECT DISTINCT e.userId FROM UserEvent e "
            + "WHERE e.eventType = com.carecode.core.analytics.EventType.SIGNED_UP "
            + "AND e.occurredDate = :date AND e.userId IS NOT NULL")
    List<Long> findUserIdsSignedUpOn(@Param("date") LocalDate date);

    /** 주어진 사용자들 중 특정 날짜에 활동한 사람 수. */
    @Query("SELECT COUNT(DISTINCT e.userId) FROM UserEvent e "
            + "WHERE e.userId IN :userIds AND e.occurredDate = :date")
    long countActiveOn(@Param("userIds") List<Long> userIds, @Param("date") LocalDate date);

    /** 이벤트 종류별 발생 건수. */
    @Query("SELECT e.eventType, COUNT(e) FROM UserEvent e "
            + "WHERE e.occurredDate BETWEEN :from AND :to GROUP BY e.eventType")
    List<Object[]> countByType(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
