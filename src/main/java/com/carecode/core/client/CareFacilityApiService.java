package com.carecode.core.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 돌봄시설 공공데이터 API 서비스
 * 보육시설, 어린이집 등의 정보를 공공데이터 포털에서 가져옴
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CareFacilityApiService {

    private final PublicDataApiClient publicDataApiClient;
    private final ObjectMapper objectMapper;
    
    @Value("${public.data.api.key}")
    private String apiKey;

    /**
     * 보육시설 정보 조회 (서울시 공공데이터 API)
     * 서울시 API는 지역별 필터링을 지원하지 않으므로 전체 서울시 데이터를 가져옵니다.
     * @param sido 시도명 (예: 서울특별시, 경기도) - 현재는 무시됨
     * @param sigungu 시군구명 (예: 강남구, 수원시) - 현재는 무시됨
     * @param pageNo 페이지 번호
     * @param numOfRows 한 페이지 결과 수 (최대 1000건)
     * @return 정제된 보육시설 정보
     */
    public Map<String, Object> getChildcareFacilities(String sido, String sigungu, int pageNo, int numOfRows) {
        // 서울시 API는 한 번에 최대 1000건까지만 요청 가능
        if (numOfRows > 1000) {
            numOfRows = 1000;
        }

        // 서울시 API는 파라미터가 URL 경로에 포함됨
        // URL 구조: /서비스명/START_INDEX/END_INDEX/
        String startIndex = String.valueOf((pageNo - 1) * numOfRows + 1);
        String endIndex = String.valueOf(pageNo * numOfRows);

        // 서울시 공공데이터 API 엔드포인트 (경로 파라미터 포함)
        String endpoint = "TnFcltySttusInfo1004/" + startIndex + "/" + endIndex + "/";

        // 서울시 API는 쿼리 파라미터가 없음
        String response = publicDataApiClient.get(endpoint, null, String.class);

        // 응답 정제 및 파싱
        Map<String, Object> result = parseAndRefineResponse(response, "TnFcltySttusInfo1004");

        return result;
    }

    /**
     * API 응답을 파싱하고 정제하여 사용하기 쉬운 형태로 변환
     */
    private Map<String, Object> parseAndRefineResponse(String rawResponse, String serviceName) {
        try {
            log.debug("원본 응답: {}", rawResponse);

            // 응답이 너무 짧으면 빈 데이터로 간주
            if (rawResponse == null || rawResponse.trim().length() < 50) {
                log.warn("응답이 너무 짧습니다. 빈 데이터로 처리합니다: {}", rawResponse);
                return createEmptyResponse(serviceName);
            }
            
            // JSON 파싱
            JsonNode rootNode = objectMapper.readTree(rawResponse);
            
            // 에러 응답 확인
            if (rootNode.has("RESULT")) {
                JsonNode resultNode = rootNode.get("RESULT");
                if (resultNode.has("CODE")) {
                    String code = resultNode.get("CODE").asText();
                    String message = resultNode.has("MESSAGE") ? resultNode.get("MESSAGE").asText() : "";
                    
                    if (code.startsWith("ERROR")) {
                        log.warn("API 에러 응답: code={}, message={}", code, message);
                        return createErrorResponse(code, message, serviceName);
                    } else if ("INFO-200".equals(code)) {
                        log.info("해당하는 데이터 없음: {}", message);
                        return createEmptyResponse(serviceName);
                    }
                }
            }
            
            JsonNode serviceNode = rootNode.get(serviceName);
            
            if (serviceNode == null) {
                log.warn("서비스 데이터를 찾을 수 없습니다: {}. 전체 응답: {}", serviceName, rawResponse);
                return createEmptyResponse(serviceName);
            }
            
            // 결과 정보 추출
            JsonNode resultNode = serviceNode.get("RESULT");
            String code = resultNode.get("CODE").asText();
            String message = resultNode.get("MESSAGE").asText();
            
            // 전체 개수 추출
            int totalCount = serviceNode.has("list_total_count") ? serviceNode.get("list_total_count").asInt() : 0;
            
            // 시설 데이터 추출 및 정제
            JsonNode rowNode = serviceNode.get("row");
            List<Map<String, Object>> facilities = new ArrayList<>();
            
            if (rowNode != null && rowNode.isArray()) {
                for (JsonNode facilityNode : rowNode) {
                    Map<String, Object> facility = new HashMap<>();
                    
                    // 주요 정보만 추출하여 정제
                    facility.put("facilityId", getNodeText(facilityNode, "FCLTY_ID"));
                    facility.put("facilityName", getNodeText(facilityNode, "FCLTY_NM"));
                    facility.put("serviceType", getNodeText(facilityNode, "SVC_CL_NM"));
                    facility.put("district", getNodeText(facilityNode, "ATDRC_NM"));
                    facility.put("ageGroup", getNodeText(facilityNode, "AGE_SE_NM"));
                    facility.put("latitude", getNodeText(facilityNode, "Y_CRDNT_VALUE"));
                    facility.put("longitude", getNodeText(facilityNode, "X_CRDNT_VALUE"));
                    facility.put("zipCode", getNodeText(facilityNode, "ZIP"));
                    facility.put("address", getNodeText(facilityNode, "BASS_ADRES"));
                    facility.put("detailAddress", getNodeText(facilityNode, "DETAIL_ADRES"));
                    facility.put("hasWebsite", "Y".equals(getNodeText(facilityNode, "SITE_ENNC")));
                    facility.put("websiteUrl", getNodeText(facilityNode, "SITE_URL"));
                    facility.put("isFree", "Y".equals(getNodeText(facilityNode, "RNTFEE_FREE_AT")));
                    facility.put("rentFee", getNodeText(facilityNode, "RNTFEE"));
                    facility.put("weekdayStartTime", formatTime(getNodeText(facilityNode, "WKDAY_BEGIN_TIME")));
                    facility.put("weekdayEndTime", formatTime(getNodeText(facilityNode, "WKDAY_END_TIME")));
                    facility.put("operationType", getNodeText(facilityNode, "USE_TIME_SE_NM"));
                    facility.put("hasSaturdayOperation", "Y".equals(getNodeText(facilityNode, "SAT_OPER_AT")));
                    facility.put("saturdayStartTime", formatTime(getNodeText(facilityNode, "SAT_OPER_BEGIN_TIME")));
                    facility.put("saturdayEndTime", formatTime(getNodeText(facilityNode, "SAT_OPER_END_TIME")));
                    facility.put("registrationDate", getNodeText(facilityNode, "REGIST_DT"));
                    facility.put("updateDate", getNodeText(facilityNode, "UPDT_DT"));
                    
                    facilities.add(facility);
                }
            }
            
            // 최종 결과 구성
            Map<String, Object> result = new HashMap<>();
            result.put("success", "INFO-000".equals(code));
            result.put("code", code);
            result.put("message", message);
            result.put("totalCount", totalCount);
            result.put("facilities", facilities);
            result.put("serviceName", serviceName);
            
            return result;

        } catch (Exception e) {
            log.error("응답 파싱 실패: {}", e.getMessage(), e);
            log.error("원본 응답: {}", rawResponse);
            return createErrorResponse("PARSE_ERROR", "응답 파싱 실패: " + e.getMessage(), serviceName);
        }
    }
    
    /**
     * 빈 응답 생성
     */
    private Map<String, Object> createEmptyResponse(String serviceName) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("code", "INFO-200");
        result.put("message", "해당하는 데이터 없음");
        result.put("totalCount", 0);
        result.put("facilities", new ArrayList<>());
        result.put("serviceName", serviceName);
        return result;
    }
    
    /**
     * 에러 응답 생성
     */
    private Map<String, Object> createErrorResponse(String code, String message, String serviceName) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("code", code);
        result.put("message", message);
        result.put("totalCount", 0);
        result.put("facilities", new ArrayList<>());
        result.put("serviceName", serviceName);
        return result;
    }
    
    /**
     * JsonNode에서 텍스트 값을 안전하게 추출
     */
    private String getNodeText(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asText() : "";
    }
    
    /**
     * 시간 형식을 HH:MM으로 변환
     */
    private String formatTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return "";
        }
        
        try {
            // HHMM 형식을 HH:MM으로 변환
            if (timeStr.length() == 4) {
                return timeStr.substring(0, 2) + ":" + timeStr.substring(2, 4);
            }
            return timeStr;
        } catch (Exception e) {
            return timeStr;
        }
    }

    /**
     * 유치원 정보 조회
     */
    public Map<String, Object> getKindergartens(String sido, String sigungu, int pageNo, int numOfRows) {
        String startIndex = String.valueOf((pageNo - 1) * numOfRows + 1);
        String endIndex = String.valueOf(pageNo * numOfRows);
        String endpoint = "/15001061/2/1/kindergarten/" + startIndex + "/" + endIndex + "/";
        String response = publicDataApiClient.get(endpoint, null, String.class);

        return parseAndRefineResponse(response, "kindergarten");
    }

    /**
     * 돌봄시설 통계 정보 조회
     */
    public Map<String, Object> getCareFacilityStatistics(String sido) {
        String endpoint = "/statistics/" + sido + "/";
        String response = publicDataApiClient.get(endpoint, null, String.class);

        return parseAndRefineResponse(response, "statistics");
    }

    /**
     * 돌봄시설 키워드 검색
     */
    public Map<String, Object> searchCareFacilities(String keyword, int pageNo, int numOfRows) {
        String startIndex = String.valueOf((pageNo - 1) * numOfRows + 1);
        String endIndex = String.valueOf(pageNo * numOfRows);
        String endpoint = "/search/" + keyword + "/" + startIndex + "/" + endIndex + "/";
        String response = publicDataApiClient.get(endpoint, null, String.class);

        return parseAndRefineResponse(response, "search");
    }
}
