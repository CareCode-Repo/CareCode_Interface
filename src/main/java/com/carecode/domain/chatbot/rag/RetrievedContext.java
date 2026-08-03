package com.carecode.domain.chatbot.rag;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 챗봇 답변의 근거로 사용할 검색 결과.
 */
@Getter
@Builder
public class RetrievedContext {

    /** 프롬프트에 넣을 문서 조각. */
    private final List<Snippet> snippets;

    public boolean isEmpty() {
        return snippets == null || snippets.isEmpty();
    }

    @Getter
    @Builder
    public static class Snippet {
        /** 출처 종류 (정책/시설/병원 등). */
        private final String source;
        private final String title;
        private final String content;
    }
}
