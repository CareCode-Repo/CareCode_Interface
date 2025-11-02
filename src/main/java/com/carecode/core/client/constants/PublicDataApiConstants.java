package com.carecode.core.client.constants;

/**
 * 공공데이터 API 관련 상수
 * API 엔드포인트, 파라미터명, 응답 코드 등을 상수로 관리
 */
public class PublicDataApiConstants {

    // API 응답 코드
    public static final String SUCCESS_CODE = "00";
    public static final String ERROR_CODE = "99";

    // API 파라미터명
    public static final String PARAM_SERVICE_KEY = "serviceKey";
    public static final String PARAM_PAGE_NO = "pageNo";
    public static final String PARAM_NUM_OF_ROWS = "numOfRows";
    public static final String PARAM_TYPE = "type";
    public static final String PARAM_SIDO_CD = "sidoCd";
    public static final String PARAM_POLICY_TYPE = "policyType";
    public static final String PARAM_EDUCATION_TYPE = "educationType";

    // API 응답 타입
    public static final String RESPONSE_TYPE_JSON = "json";
    public static final String RESPONSE_TYPE_XML = "xml";

    // 육아 관련 API 엔드포인트
    public static final String ENDPOINT_CHILDCARE_FACILITIES = "/getChildcareFacilities";
    public static final String ENDPOINT_CHILDCARE_POLICIES = "/getChildcarePolicies";
    public static final String ENDPOINT_PEDIATRIC_HOSPITALS = "/getPediatricHospitals";
    public static final String ENDPOINT_CHILDCARE_SUBSIDIES = "/getChildcareSubsidies";
    public static final String ENDPOINT_CHILDCARE_EDUCATION = "/getChildcareEducation";

    // 지역 코드
    public static final String REGION_SEOUL = "11";
    public static final String REGION_BUSAN = "21";
    public static final String REGION_DAEGU = "22";
    public static final String REGION_INCHEON = "23";
    public static final String REGION_GWANGJU = "24";
    public static final String REGION_DAEJEON = "25";
    public static final String REGION_ULSAN = "26";
    public static final String REGION_SEJONG = "29";
    public static final String REGION_GYEONGGI = "31";
    public static final String REGION_GANGWON = "32";
    public static final String REGION_CHUNGBUK = "33";
    public static final String REGION_CHUNGNAM = "34";
    public static final String REGION_JEONBUK = "35";
    public static final String REGION_JEONNAM = "36";
    public static final String REGION_GYEONGBUK = "37";
    public static final String REGION_GYEONGNAM = "38";
    public static final String REGION_JEJU = "39";

    // 정책 유형
    public static final String POLICY_TYPE_SUBSIDY = "subsidy";
    public static final String POLICY_TYPE_FACILITY = "facility";
    public static final String POLICY_TYPE_EDUCATION = "education";
    public static final String POLICY_TYPE_MEDICAL = "medical";

    // 교육 유형
    public static final String EDUCATION_TYPE_PARENTING = "parenting";
    public static final String EDUCATION_TYPE_CHILDCARE = "childcare";
    public static final String EDUCATION_TYPE_SAFETY = "safety";
    public static final String EDUCATION_TYPE_HEALTH = "health";

    // 기본 페이지 설정
    public static final int DEFAULT_PAGE_NO = 1;
    public static final int DEFAULT_NUM_OF_ROWS = 10;
    public static final int MAX_NUM_OF_ROWS = 1000;

    // 타임아웃 설정 (밀리초)
    public static final int CONNECT_TIMEOUT = 10000;
    public static final int READ_TIMEOUT = 30000;

    // 에러 메시지
    public static final String ERROR_MSG_API_CALL_FAILED = "API 호출에 실패했습니다.";
    public static final String ERROR_MSG_INVALID_RESPONSE = "잘못된 응답입니다.";
    public static final String ERROR_MSG_TIMEOUT = "요청 시간이 초과되었습니다.";
    public static final String ERROR_MSG_NETWORK_ERROR = "네트워크 오류가 발생했습니다.";

    private PublicDataApiConstants() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }
} 