package com.carecode.domain.admin.dto.response;

import com.carecode.core.ops.sync.SyncFreshnessService.JobFreshness;

import java.util.List;

/**
 * 주기 작업 상태 목록.
 *
 * <p>{@code staleCount} 를 함께 주는 이유: 목록이 길어서 화면이 전부 접어 두는데, 하나라도
 * 기준을 넘겼는지는 접힌 상태에서도 보여야 한다.
 */
public record SyncStatusResponse(List<JobFreshness> jobs, long staleCount) {

    public static SyncStatusResponse of(List<JobFreshness> jobs) {
        return new SyncStatusResponse(jobs, jobs.stream().filter(JobFreshness::isStale).count());
    }
}
