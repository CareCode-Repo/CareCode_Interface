package com.carecode.core.devtools;

/** 샘플 데이터 식별자. 실데이터와 섞이지 않도록 접두어로 구분하고, 이 접두어로 일괄 삭제한다. */
public final class SampleDataProperties {

    /** 샘플 정책 코드 접두어. */
    public static final String POLICY_PREFIX = "SAMPLE-POLICY-";

    /** 샘플 시설 코드 접두어. */
    public static final String FACILITY_PREFIX = "SAMPLE-FACILITY-";

    private SampleDataProperties() {
    }
}
