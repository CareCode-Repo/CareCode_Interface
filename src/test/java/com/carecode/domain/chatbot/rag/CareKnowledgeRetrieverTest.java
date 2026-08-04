package com.carecode.domain.chatbot.rag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("챗봇 키워드 추출")
class CareKnowledgeRetrieverTest {

    private final CareKnowledgeRetriever retriever = new CareKnowledgeRetriever(null, null);

    @Test
    @DisplayName("조사를 떼어내고 원형을 남긴다")
    void stripsJosa() {
        List<String> keywords = retriever.extractKeywords("강남구에서 어린이집은 어디에 있나요");

        assertThat(keywords).contains("어린이집");
        assertThat(keywords).noneMatch(k -> k.endsWith("에서"));
    }

    @Test
    @DisplayName("의문사·범용어는 검색어에서 제외한다")
    void dropsStopWords() {
        List<String> keywords = retriever.extractKeywords("부모급여 정보 알려줘");

        assertThat(keywords).containsExactly("부모급여");
    }

    @Test
    @DisplayName("긴 토큰을 먼저 시도하도록 정렬한다")
    void ordersByLengthDescending() {
        List<String> keywords = retriever.extractKeywords("아동수당 신청");

        assertThat(keywords).containsExactly("아동수당", "신청");
    }

    @Test
    @DisplayName("키워드는 최대 3개까지만 뽑는다")
    void limitsKeywordCount() {
        List<String> keywords = retriever.extractKeywords("서울 강남 어린이집 보육료 지원 신청 방법");

        assertThat(keywords).hasSize(3);
    }

    @Test
    @DisplayName("빈 질문이면 검색을 건너뛴다")
    void returnsEmptyForBlankQuestion() {
        assertThat(retriever.extractKeywords("  ")).isEmpty();
        assertThat(retriever.extractKeywords(null)).isEmpty();
    }
}
