package com.carecode.domain.policy.repository;

import com.carecode.domain.policy.entity.PolicyDeadlineNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PolicyDeadlineNoticeRepository extends JpaRepository<PolicyDeadlineNotice, Long> {

    /**
     * 오늘 이 정책으로 이미 알림을 받은 사용자들.
     *
     * <p>사용자마다 한 번씩 물으면 대상자 수만큼 질의가 나가므로 한 번에 가져온다.
     */
    @Query("SELECT n.userId FROM PolicyDeadlineNotice n "
            + "WHERE n.policyId = :policyId AND n.notifiedOn = :notifiedOn")
    List<Long> findNotifiedUserIds(@Param("policyId") Long policyId,
                                   @Param("notifiedOn") LocalDate notifiedOn);
}
