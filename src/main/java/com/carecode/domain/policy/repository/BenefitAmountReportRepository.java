package com.carecode.domain.policy.repository;

import com.carecode.domain.policy.entity.BenefitAmountReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BenefitAmountReportRepository extends JpaRepository<BenefitAmountReport, Long> {

    Optional<BenefitAmountReport> findByPolicyIdAndUserId(Long policyId, Long userId);

    List<BenefitAmountReport> findByPolicyId(Long policyId);

    /** 같은 금액·지급방식을 몇 명이 제보했는지. 합의가 이뤄진 값만 채택한다. */
    @Query("SELECT r.reportedAmount, r.paymentType, COUNT(r) FROM BenefitAmountReport r "
            + "WHERE r.policyId = :policyId "
            + "GROUP BY r.reportedAmount, r.paymentType ORDER BY COUNT(r) DESC")
    List<Object[]> countByAmountAndType(@Param("policyId") Long policyId);
}
