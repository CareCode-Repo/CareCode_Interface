package com.carecode.domain.policy.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** 개인화 추천 결과 한 건. 왜 추천됐는지 근거를 함께 내려 준다. */
@Getter
@Builder
public class PersonalizedPolicyResponse {
    private PolicyDto policy;
    private int score;
    private List<String> reasons;
}
