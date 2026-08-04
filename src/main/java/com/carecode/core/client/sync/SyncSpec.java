package com.carecode.core.client.sync;

import com.carecode.core.client.provider.PublicDataProvider;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/** 페이징 동기화 한 건의 명세. 데이터셋마다 다른 부분만 여기에 담는다. */
@Getter
@Builder
public class SyncSpec {

    private final PublicDataProvider provider;

    /** 공급자 기준 상대 경로 또는 절대 URL. */
    private final String resource;

    /** 로그·응답에 쓰는 사람이 읽는 이름. */
    private final String label;

    private final int rowsPerPage;

    @Builder.Default
    private final Map<String, String> params = Map.of();

    /** 적재 대상이 아닌 행을 걸러낸다. 기본은 전부 적재. */
    @Builder.Default
    private final Predicate<JsonNode> filter = row -> true;

    /** 한 건 저장. 신규면 true, 갱신이면 false 를 반환한다. */
    private final Function<JsonNode, Boolean> upsert;
}
