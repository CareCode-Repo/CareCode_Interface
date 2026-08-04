package com.carecode.core.client.provider;

import java.util.Map;

/** 공공데이터 공급자 추상화. 공급자별 인증 방식과 URL 규격 차이를 구현체가 흡수한다. */
public interface PublicDataProvider {

    String getProviderName();

    /** API 키가 없으면 false. 호출부가 조용히 건너뛴다. */
    boolean isAvailable();

    /** 원본 응답 본문을 반환한다. pageNo는 1부터 시작. */
    String fetch(String resource, int pageNo, int numOfRows, Map<String, String> params);
}
