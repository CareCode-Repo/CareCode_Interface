package com.carecode.domain.policy.repository;

import com.carecode.domain.policy.entity.PolicyChange;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PolicyChangeRepository extends JpaRepository<PolicyChange, Long> {

    /** 아직 알리지 않은 변경. 오래된 것부터 처리한다. */
    @Query("SELECT c FROM PolicyChange c WHERE c.notified = false ORDER BY c.detectedAt ASC")
    List<PolicyChange> findUnnotified(Pageable pageable);

    /** 최근 변경 내역. 사용자에게 "이번 달 달라진 지원금" 으로 보여준다. */
    @Query("SELECT c FROM PolicyChange c WHERE c.detectedAt >= :since ORDER BY c.detectedAt DESC")
    List<PolicyChange> findRecent(@Param("since") LocalDateTime since, Pageable pageable);
}
