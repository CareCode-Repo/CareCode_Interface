package com.carecode.domain.community.repository;

import com.carecode.domain.community.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId,
                                                       Report.TargetType targetType,
                                                       Long targetId);

    Page<Report> findByStatusOrderByCreatedAtAsc(Report.ReportStatus status, Pageable pageable);

    /** 대상별 누적 신고 수. 자동 숨김 임계치 판단에 사용한다. */
    @Query("SELECT COUNT(r) FROM Report r " +
           "WHERE r.targetType = :targetType AND r.targetId = :targetId " +
           "AND r.status <> com.carecode.domain.community.entity.Report.ReportStatus.REJECTED")
    long countActiveReports(@Param("targetType") Report.TargetType targetType,
                            @Param("targetId") Long targetId);
}
