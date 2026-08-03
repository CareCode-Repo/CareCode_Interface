package com.carecode.domain.health.repository;

import com.carecode.domain.health.entity.VaccinationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VaccinationScheduleRepository extends JpaRepository<VaccinationSchedule, Long> {

    List<VaccinationSchedule> findByChildIdOrderByDueDateAsc(Long childId);

    boolean existsByChildId(Long childId);

    /**
     * 알림 대상 조회: 예정일이 구간 안에 있고 아직 알림을 보내지 않은 미완료 일정.
     *
     * <p>child, user 를 함께 로딩해 알림 발송 시 N+1 을 피한다.
     */
    @Query("SELECT vs FROM VaccinationSchedule vs " +
           "JOIN FETCH vs.child c JOIN FETCH c.user " +
           "WHERE vs.status = com.carecode.domain.health.entity.VaccinationSchedule.VaccinationStatus.SCHEDULED " +
           "AND vs.reminderSentAt IS NULL " +
           "AND vs.dueDate BETWEEN :from AND :to")
    List<VaccinationSchedule> findPendingReminders(@Param("from") LocalDate from,
                                                   @Param("to") LocalDate to);

    @Query("SELECT vs FROM VaccinationSchedule vs " +
           "WHERE vs.child.id = :childId " +
           "AND vs.status = com.carecode.domain.health.entity.VaccinationSchedule.VaccinationStatus.SCHEDULED " +
           "AND vs.dueDate < :today ORDER BY vs.dueDate ASC")
    List<VaccinationSchedule> findOverdue(@Param("childId") Long childId,
                                          @Param("today") LocalDate today);
}
