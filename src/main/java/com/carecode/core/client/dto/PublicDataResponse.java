package com.carecode.core.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 공공데이터 API 공통 응답 DTO
 * 대부분의 공공데이터 API가 공통적으로 사용하는 응답 구조
 */
@Data
@NoArgsConstructor
public class PublicDataResponse<T> {

    @JsonProperty("response")
    private Response<T> response;

    @Data
    @NoArgsConstructor
    public static class Response<T> {
        @JsonProperty("header")
        private Header header;

        @JsonProperty("body")
        private Body<T> body;
    }

    @Data
    @NoArgsConstructor
    public static class Header {
        @JsonProperty("resultCode")
        private String resultCode;

        @JsonProperty("resultMsg")
        private String resultMsg;
    }

    @Data
    @NoArgsConstructor
    public static class Body<T> {
        @JsonProperty("items")
        private Items<T> items;

        @JsonProperty("numOfRows")
        private int numOfRows;

        @JsonProperty("pageNo")
        private int pageNo;

        @JsonProperty("totalCount")
        private int totalCount;
    }

    @Data
    @NoArgsConstructor
    public static class Items<T> {
        @JsonProperty("item")
        private List<T> item;
    }

    /**
     * 응답이 성공인지 확인
     * @return 성공 여부
     */
    public boolean isSuccess() {
        return response != null && 
               response.getHeader() != null && 
               "00".equals(response.getHeader().getResultCode());
    }

    /**
     * 에러 메시지 반환
     * @return 에러 메시지
     */
    public String getErrorMessage() {
        if (response != null && response.getHeader() != null) {
            return response.getHeader().getResultMsg();
        }
        return "Unknown error";
    }

    /**
     * 데이터 목록 반환
     * @return 데이터 목록
     */
    public List<T> getData() {
        if (response != null && 
            response.getBody() != null && 
            response.getBody().getItems() != null) {
            return response.getBody().getItems().getItem();
        }
        return null;
    }

    /**
     * 총 개수 반환
     * @return 총 개수
     */
    public int getTotalCount() {
        if (response != null && response.getBody() != null) {
            return response.getBody().getTotalCount();
        }
        return 0;
    }
} 