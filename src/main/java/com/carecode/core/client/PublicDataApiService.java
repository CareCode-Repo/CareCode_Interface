package com.carecode.core.client;

import com.carecode.core.client.dto.PublicDataResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 공공데이터 API 서비스
 * 구체적인 API 호출 로직과 비즈니스 로직을 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicDataApiService {

    private final PublicDataApiClient apiClient;

    /**
     * 육아 관련 시설 정보 조회
     * @param region 지역명
     * @param pageNo 페이지 번호
     * @param numOfRows 페이지당 행 수
     * @return 육아 시설 정보
     */
    public PublicDataResponse<Object> getChildcareFacilities(String region, int pageNo, int numOfRows) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("pageNo", String.valueOf(pageNo));
            params.put("numOfRows", String.valueOf(numOfRows));
            params.put("type", "json");
            
            if (region != null && !region.isEmpty()) {
                params.put("sidoCd", region);
            }

            return apiClient.get("/getChildcareFacilities", params, PublicDataResponse.class);
        } catch (Exception e) {
            log.error("육아 시설 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("육아 시설 정보 조회 실패", e);
        }
    }

    /**
     * 보육 정책 정보 조회
     * @param policyType 정책 유형
     * @param pageNo 페이지 번호
     * @param numOfRows 페이지당 행 수
     * @return 보육 정책 정보
     */
    public PublicDataResponse<Object> getChildcarePolicies(String policyType, int pageNo, int numOfRows) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("pageNo", String.valueOf(pageNo));
            params.put("numOfRows", String.valueOf(numOfRows));
            params.put("type", "json");
            
            if (policyType != null && !policyType.isEmpty()) {
                params.put("policyType", policyType);
            }

            return apiClient.get("/getChildcarePolicies", params, PublicDataResponse.class);
        } catch (Exception e) {
            log.error("보육 정책 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("보육 정책 정보 조회 실패", e);
        }
    }

    /**
     * 소아과 병원 정보 조회
     * @param region 지역명
     * @param pageNo 페이지 번호
     * @param numOfRows 페이지당 행 수
     * @return 소아과 병원 정보
     */
    public PublicDataResponse<Object> getPediatricHospitals(String region, int pageNo, int numOfRows) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("pageNo", String.valueOf(pageNo));
            params.put("numOfRows", String.valueOf(numOfRows));
            params.put("type", "json");
            
            if (region != null && !region.isEmpty()) {
                params.put("sidoCd", region);
            }

            return apiClient.get("/getPediatricHospitals", params, PublicDataResponse.class);
        } catch (Exception e) {
            log.error("소아과 병원 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("소아과 병원 정보 조회 실패", e);
        }
    }

    /**
     * 육아 지원금 정보 조회
     * @param region 지역명
     * @param pageNo 페이지 번호
     * @param numOfRows 페이지당 행 수
     * @return 육아 지원금 정보
     */
    public PublicDataResponse<Object> getChildcareSubsidies(String region, int pageNo, int numOfRows) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("pageNo", String.valueOf(pageNo));
            params.put("numOfRows", String.valueOf(numOfRows));
            params.put("type", "json");
            
            if (region != null && !region.isEmpty()) {
                params.put("sidoCd", region);
            }

            return apiClient.get("/getChildcareSubsidies", params, PublicDataResponse.class);
        } catch (Exception e) {
            log.error("육아 지원금 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("육아 지원금 정보 조회 실패", e);
        }
    }

    /**
     * 육아 관련 교육 정보 조회
     * @param educationType 교육 유형
     * @param pageNo 페이지 번호
     * @param numOfRows 페이지당 행 수
     * @return 육아 교육 정보
     */
    public PublicDataResponse<Object> getChildcareEducation(String educationType, int pageNo, int numOfRows) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("pageNo", String.valueOf(pageNo));
            params.put("numOfRows", String.valueOf(numOfRows));
            params.put("type", "json");
            
            if (educationType != null && !educationType.isEmpty()) {
                params.put("educationType", educationType);
            }

            return apiClient.get("/getChildcareEducation", params, PublicDataResponse.class);
        } catch (Exception e) {
            log.error("육아 교육 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("육아 교육 정보 조회 실패", e);
        }
    }

    /**
     * 커스텀 API 호출
     * @param endpoint API 엔드포인트
     * @param params 파라미터
     * @return API 응답
     */
    public PublicDataResponse<Object> callCustomApi(String endpoint, Map<String, String> params) {
        try {
            if (params == null) {
                params = new HashMap<>();
            }
            params.put("type", "json");

            return apiClient.get(endpoint, params, PublicDataResponse.class);
        } catch (Exception e) {
            log.error("커스텀 API 호출 중 오류 발생: {} - {}", endpoint, e.getMessage(), e);
            throw new RuntimeException("커스텀 API 호출 실패", e);
        }
    }

    /**
     * API 응답 검증
     * @param response API 응답
     * @return 검증 결과
     */
    public boolean validateResponse(PublicDataResponse<Object> response) {
        if (response == null) {
            log.error("API 응답이 null입니다.");
            return false;
        }

        if (!response.isSuccess()) {
            log.error("API 호출 실패: {}", response.getErrorMessage());
            return false;
        }

        return true;
    }
} 