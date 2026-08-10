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
import java.util.Comparator;
import java.util.List;

/** 챗봇 답변 근거 검색기(RAG 의 R). 임베딩 검색으로 바꾸려면 이 클래스 내부만 교체하면 된다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CareKnowledgeRetriever {

    private static final int MAX_PER_SOURCE = 3;
    private static final int MAX_CONTENT_LENGTH = 400;
    private static final int MAX_KEYWORDS = 3;

    /** 검색어에서 제외할 의문사·범용어. 이걸로 검색하면 아무거나 매칭된다. */
    private static final List<String> STOP_WORDS = List.of(
            "알려줘", "알려주세요", "어디", "무엇", "뭐가", "뭔가요", "있나요", "있어",
            "해줘", "하고", "그리고", "관련", "대해", "대한", "정보", "추천",
            "우리", "저희", "지금", "가까운", "근처");

    /** 길이 내림차순. 긴 것부터 떼어내야 "에서" 가 "서" 로 잘리지 않는다. */
    private static final List<String> JOSA = List.of(
            "에서는", "으로는", "에게는", "에서", "으로", "에게", "부터", "까지", "이랑",
            "은", "는", "이", "가", "을", "를", "의", "에", "도", "와", "과", "로");

    private final PolicyRepository policyRepository;
    private final CareFacilityRepository careFacilityRepository;

    @Transactional(readOnly = true)
    public RetrievedContext retrieve(String question) {
        List<String> keywords = extractKeywords(question);
        if (keywords.isEmpty()) {
            return RetrievedContext.builder().snippets(List.of()).build();
        }

        // 변별력 높은 키워드부터 시도하고, 근거를 찾으면 멈춘다.
        List<RetrievedContext.Snippet> snippets = new ArrayList<>();
        for (String keyword : keywords) {
            try {
                snippets.addAll(searchPolicies(keyword));
                snippets.addAll(searchFacilities(keyword));
            } catch (Exception e) {
                // 검색 실패가 대화를 막지 않도록 한다. 근거 없이 일반 답변으로 진행한다.
                log.error("챗봇 근거 검색 실패 - keyword={}", keyword, e);
            }
            if (!snippets.isEmpty()) {
                break;
            }
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

    /** 질문에서 검색어 후보를 길이 내림차순으로 뽑는다. 긴 토큰일수록 변별력이 높다. */
    List<String> extractKeywords(String question) {
        if (question == null || question.isBlank()) {
            return List.of();
        }

        String cleaned = question.replaceAll("[^가-힣a-zA-Z0-9\\s]", " ");
        List<String> keywords = new ArrayList<>();
        for (String token : cleaned.split("\\s+")) {
            String normalized = stripJosa(token);
            if (normalized.length() < 2 || STOP_WORDS.contains(normalized) || keywords.contains(normalized)) {
                continue;
            }
            keywords.add(normalized);
        }
        keywords.sort(Comparator.comparingInt(String::length).reversed());
        return keywords.size() > MAX_KEYWORDS ? keywords.subList(0, MAX_KEYWORDS) : keywords;
    }

    /** 흔한 조사를 떼어낸다. */
    private String stripJosa(String token) {
        for (String josa : JOSA) {
            if (token.length() > josa.length() + 1 && token.endsWith(josa)) {
                return token.substring(0, token.length() - josa.length());
            }
        }
        return token;
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= MAX_CONTENT_LENGTH ? text : text.substring(0, MAX_CONTENT_LENGTH) + "...";
    }
}
