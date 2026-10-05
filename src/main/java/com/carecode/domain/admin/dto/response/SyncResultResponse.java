package com.carecode.domain.admin.dto.response;

import com.carecode.external.publicdata.sync.SyncResult;

/**
 * 공공데이터 동기화 실행 결과.
 *
 * <p>{@code skipped}(필터에 걸려 적재하지 않음)를 {@code failed}(오류) 와 따로 센다. 둘을 합치면
 * "필터가 과도한가" 와 "외부 API 가 깨졌나" 를 구분할 수 없다.
 *
 * <p>{@code completed} 가 false 면 {@code stoppedReason} 에 중단 사유가 있다. 중간에 멈춘 실행을
 * 성공으로 읽으면, 실제로는 일부만 들어온 데이터를 전체로 믿게 된다.
 */
public record SyncResultResponse(
        String provider,
        String resource,
        boolean completed,
        int created,
        int updated,
        int failed,
        int skipped,
        int pagesProcessed,
        String stoppedReason) {

    public static SyncResultResponse from(SyncResult result) {
        return new SyncResultResponse(
                result.getProvider(),
                result.getResource(),
                result.isCompleted(),
                result.getCreated(),
                result.getUpdated(),
                result.getFailed(),
                result.getSkipped(),
                result.getPagesProcessed(),
                result.getStoppedReason());
    }
}
