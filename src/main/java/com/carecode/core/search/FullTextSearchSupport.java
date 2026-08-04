package com.carecode.core.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 전문 검색 사용 가능 여부와 키워드 정규화. H2 등 MATCH AGAINST 미지원 DB에서는 끌 수 있다. */
@Slf4j
@Component
public class FullTextSearchSupport {

    /** FULLTEXT 인덱스는 너무 짧은 토큰을 무시하므로 그 이하는 LIKE 로 처리한다. */
    private static final int MIN_KEYWORD_LENGTH = 2;

    private final boolean enabled;

    public FullTextSearchSupport(@Value("${app.search.fulltext-enabled:true}") boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            log.info("전문 검색이 비활성화되어 LIKE 검색으로 동작합니다.");
        }
    }

    /** 이 키워드를 전문 검색으로 처리할 수 있는지. */
    public boolean canUseFullText(String keyword) {
        return enabled && normalize(keyword) != null;
    }

    /** 검색 연산자로 해석될 수 있는 문자를 제거한다. 제거 후 유효하지 않으면 null. */
    public String normalize(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        // +, -, *, ", ~, <, >, ( ) 는 불리언 모드 연산자이거나 파싱을 깨뜨린다.
        String cleaned = keyword.replaceAll("[+\\-*\"~<>()@]", " ").replaceAll("\\s+", " ").trim();
        return cleaned.length() >= MIN_KEYWORD_LENGTH ? cleaned : null;
    }
}
