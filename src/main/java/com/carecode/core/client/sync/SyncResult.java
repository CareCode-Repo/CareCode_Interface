package com.carecode.core.client.sync;

import lombok.Getter;

/** 공공데이터 동기화 결과. */
@Getter
public class SyncResult {

    private final String provider;
    private final String resource;
    private int created;
    private int updated;
    private int failed;
    private int pagesProcessed;

    /** 정상 완료가 아니면 중단 사유. 정상이면 null. */
    private String stoppedReason;

    public SyncResult(String provider, String resource) {
        this.provider = provider;
        this.resource = resource;
    }

    public void countCreated() {
        created++;
    }

    public void countUpdated() {
        updated++;
    }

    public void countFailed() {
        failed++;
    }

    public void countPage() {
        pagesProcessed++;
    }

    public void stop(String reason) {
        this.stoppedReason = reason;
    }

    public boolean isCompleted() {
        return stoppedReason == null;
    }

    public int getTotalProcessed() {
        return created + updated;
    }

    @Override
    public String toString() {
        return String.format("[%s/%s] 신규=%d, 갱신=%d, 실패=%d, 페이지=%d%s",
                provider, resource, created, updated, failed, pagesProcessed,
                stoppedReason != null ? ", 중단사유=" + stoppedReason : "");
    }
}
