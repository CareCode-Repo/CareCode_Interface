package com.carecode.core.ops.sync;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SyncRunRepository extends JpaRepository<SyncRun, Long> {

    /** 신선도 기준이 되는 "마지막으로 데이터를 갱신한 실행". 일부 실패(PARTIAL)도 갱신은 됐으므로 포함한다. */
    @Query("SELECT r FROM SyncRun r WHERE r.job = :job AND r.status IN ('SUCCESS', 'PARTIAL') "
            + "ORDER BY r.finishedAt DESC LIMIT 1")
    Optional<SyncRun> findLastFreshRun(@Param("job") String job);

    Optional<SyncRun> findFirstByJobOrderByFinishedAtDesc(String job);

    List<SyncRun> findByJobOrderByFinishedAtDesc(String job);

    /** 이력은 운영 판단용이라 오래된 것은 지운다. 정리 스케줄러가 호출한다. */
    @Modifying
    @Query("DELETE FROM SyncRun r WHERE r.finishedAt < :threshold")
    int deleteOlderThan(@Param("threshold") LocalDateTime threshold);
}
