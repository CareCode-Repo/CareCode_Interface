package com.carecode.domain.chatbot.rag;

import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.repository.CareFacilityRepository;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 챗봇 답변 근거 검색기 (RAG 의 R).
 *
 * <p>사용자 질문에서 키워드를 뽑아 DB 의 정책·시설 데이터를 조회한다.
 * 이렇게 하면 챗봇이 일반론 대신 "우리 서비스에 실제로 등록된" 내용을 근거로 답할 수 있다.
 *
 * <p>지금은 키워드 LIKE 검색이다. 데이터가 커지면 임베딩 기반 벡터 검색으로
 * 이 클래스 내부만 교체하면 된다 — 호출부는 {@link RetrievedContext} 에만 의존한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CareKnowledgeRetriever {

    private static final int MAX_PER_SOURCE = 3;
    private static final int MAX_CONTENT_LENGTH = 400;

    /** 검색어에서 제외할 조사·의문사. 이걸 키워드로 쓰면 아무거나 매칭된다. */
    private static final List<String> STOP_WORDS = List.of(
            "알려줘", "알려주세요", "어디", "무엇", "뭐가", "뭔가요", "있나요", "있어",
            "해줘", "하고", "그리고", "관련", "대해", "대한", "정보", "추천");

    private final PolicyRepository policyRepository;
    private final CareFacilityRepository careFacilityRepository;

    @Transactional(readOnly = true)
    public RetrievedContext retrieve(String question) {
        String keyword = extractKeyword(question);
        if (keyword == null) {
            return RetrievedContext.builder().snippets(List.of()).build();
        }

        List<RetrievedContext.Snippet> snippets = new ArrayList<>();
        try {
            snippets.addAll(searchPolicies(keyword));
            snippets.addAll(searchFacilities(keyword));
        } catch (Exception e) {
            // 검색 실패가 대화 자체를 막지 않도록 한다. 근거 없이 일반 답변으로 진행한다.
            log.error("챗봇 근거 검색 실패 - keyword={}", keyword, e);
        }

        return RetrievedContext.builder().snippets(snippets).build();
    }

    private List<RetrievedContext.Snippet> searchPolicies(String keyword) {
        List<Policy> policies = policyRepository
                .findBySearchCriteria(keyword, null, null, null, null, PageRequest.of(0, MAX_PER_SOURCE))
                .getContent();

        return policies.stream()
                .map(p -> RetrievedContext.Snippet.builder()
                        .source("정책")
                        .title(p.getTitle())
                        .content(buildPolicyContent(p))
                        .build())
                .toList();
    }

    private List<RetrievedContext.Snippet> searchFacilities(String keyword) {
        List<CareFacility> facilities = careFacilityRepository
                .findBySearchCriteria(keyword, null, null, PageRequest.of(0, MAX_PER_SOURCE))
                .getContent();

        return facilities.stream()
                .map(f -> RetrievedContext.Snippet.builder()
                        .source("돌봄시설")
                        .title(f.getName())
                        .content(truncate(String.format("주소: %s / 연락처: %s / 유형: %s",
                                f.getAddress(), f.getPhone(), f.getFacilityType())))
                        .build())
                .toList();
    }

    private String buildPolicyContent(Policy policy) {
        StringBuilder sb = new StringBuilder();
        if (policy.getDescription() != null) {
            sb.append(policy.getDescription());
        }
        if (policy.getBenefitAmount() != null) {
            sb.append(" / 지원금액: ").append(policy.getBenefitAmount());
        }
        if (policy.getApplicationUrl() != null) {
            sb.append(" / 신청: ").append(policy.getApplicationUrl());
        }
        return truncate(sb.toString());
    }

    /**
     * 질문에서 검색에 쓸 핵심 키워드를 뽑는다.
     * 불용어를 제거하고 가장 긴 토큰을 사용한다 — 짧은 토큰일수록 노이즈가 많다.
     */
    private String extractKeyword(String question) {
        if (question == null || question.isBlank()) {
            return null;
        }

        String cleaned = question.replaceAll("[^가-힣a-zA-Z0-9\\s]", " ");
        String best = null;
        for (String token : cleaned.split("\\s+")) {
            if (token.length() < 2 || STOP_WORDS.contains(token)) {
                continue;
            }
            if (best == null || token.length() > best.length()) {
                best = token;
            }
        }
        return best;
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= MAX_CONTENT_LENGTH ? text : text.substring(0, MAX_CONTENT_LENGTH) + "...";
    }
}
