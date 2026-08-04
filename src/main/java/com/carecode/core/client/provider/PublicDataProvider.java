package com.carecode.core.client.provider;

import java.util.Map;

/**
 * 공공데이터 공급자 추상화.
 *
 * <p>공급자마다 인증 방식과 URL 규격이 다르다.
 * <ul>
 *   <li>서울 열린데이터광장: {@code /{key}/json/{service}/{start}/{end}/} — 경로 파라미터</li>
 *   <li>공공데이터포털(data.go.kr): {@code ?serviceKey=...&pageNo=1&numOfRows=100} — 쿼리 파라미터</li>
 * </ul>
 *
 * <p>이 차이를 구현체가 흡수해서, 도메인 코드는 "몇 페이지에서 몇 건" 만 요청하면 되게 한다.
 * 새 공급자를 붙일 때 기존 코드를 건드리지 않기 위한 경계다.
 */
public interface PublicDataProvider {

    /** 공급자 식별자. 로그와 동기화 이력에 사용한다. */
    String getProviderName();

    /**
     * API 키가 설정돼 있어 실제 호출이 가능한지.
     * 키가 없으면 호출부가 조용히 건너뛴다 (로컬/CI 기동 보장).
     */
    boolean isAvailable();

    /**
     * 데이터를 조회한다.
     *
     * @param resource   공급자 내 리소스 식별자 (서비스명/오퍼레이션명)
     * @param pageNo     1부터 시작하는 페이지 번호
     * @param numOfRows  한 페이지 건수
     * @param params     추가 쿼리 파라미터 (지역 코드 등). 없으면 빈 맵
     * @return 원본 응답 본문 (JSON 문자열)
     */
    String fetch(String resource, int pageNo, int numOfRows, Map<String, String> params);
}
