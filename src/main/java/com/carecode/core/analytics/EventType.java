package com.carecode.core.analytics;

/** 추적 대상 이벤트. 제품 판단에 쓰이는 것만 남긴다 — 다 찍으면 아무것도 안 보인다. */
public enum EventType {

    // 온보딩 퍼널
    SIGNED_UP,
    CHILD_REGISTERED,
    ADDRESS_REGISTERED,
    INCOME_REGISTERED,

    // 핵심 가치 — 이 전환율이 서비스의 존재 이유를 증명한다
    MISSED_BENEFIT_VIEWED,
    BENEFIT_LINK_CLICKED,

    // 탐색
    RECOMMENDATION_VIEWED,
    REGIONAL_COMPARISON_VIEWED,
    FACILITY_VIEWED,
    ADMISSION_FORECAST_VIEWED,
    FACILITY_POPULARITY_VIEWED,

    // 유지
    APP_OPENED,
    BOOKING_CREATED,
    CHATBOT_ASKED
}
