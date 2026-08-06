package com.carecode.core.client.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 명세서 v1.0 의 응답 코드 기준. */
@DisplayName("보육통합정보 응답 코드")
class ChildcareApiStatusTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("정상 응답은 코드가 없다")
    void okWhenNoErrorCode() {
        assertThat(ChildcareApiStatus.of(node("{\"item\":[]}"))).isEqualTo(ChildcareApiStatus.OK);
        assertThat(ChildcareApiStatus.of(null)).isEqualTo(ChildcareApiStatus.OK);
    }

    @Test
    @DisplayName("검색결과 없음은 정상이라 계속 진행한다")
    void noResultIsNotFatal() {
        ChildcareApiStatus status = ChildcareApiStatus.of(
                node("{\"errcode\":\"INFO-200\",\"errmsg\":\"검색결과가 없습니다.\"}"));

        assertThat(status).isEqualTo(ChildcareApiStatus.NO_RESULT);
        assertThat(status.isFatal()).isFalse();
    }

    @Test
    @DisplayName("일 요청 한도 초과는 즉시 중단해야 한다")
    void quotaExceededIsFatal() {
        // 이걸 빈 응답으로 넘기면 남은 지역을 헛돌며 0건으로 끝난다
        ChildcareApiStatus status = ChildcareApiStatus.of(
                node("{\"errcode\":\"INFO-300\",\"errmsg\":\"일 요청 건수를 초과하였습니다.\"}"));

        assertThat(status).isEqualTo(ChildcareApiStatus.QUOTA_EXCEEDED);
        assertThat(status.isFatal()).isTrue();
    }

    @Test
    @DisplayName("인증키 무효·만료는 즉시 중단해야 한다")
    void keyProblemsAreFatal() {
        assertThat(ChildcareApiStatus.of(node("{\"errcode\":\"INFO-100\"}")).isFatal()).isTrue();
        assertThat(ChildcareApiStatus.of(node("{\"errcode\":\"INFO-400\"}")).isFatal()).isTrue();
    }

    @Test
    @DisplayName("파라미터 누락·서버 오류는 해당 지역만 건너뛴다")
    void requestErrorsAreNotFatal() {
        assertThat(ChildcareApiStatus.of(node("{\"errcode\":\"ERROR-100\"}")).isFatal()).isFalse();
        assertThat(ChildcareApiStatus.of(node("{\"errcode\":\"ERROR-200\"}")).isFatal()).isFalse();
    }

    @Test
    @DisplayName("모르는 코드는 서버 오류로 본다")
    void unknownCodeTreatedAsServerError() {
        assertThat(ChildcareApiStatus.of(node("{\"errcode\":\"XXX-999\"}")))
                .isEqualTo(ChildcareApiStatus.SERVER_ERROR);
    }

    @Test
    @DisplayName("중단 사유에 API 메시지를 담는다")
    void describesWithApiMessage() {
        JsonNode root = node("{\"errcode\":\"INFO-300\",\"errmsg\":\"일 요청 건수를 초과하였습니다.\"}");

        assertThat(ChildcareApiStatus.of(root).describe(root))
                .contains("INFO-300").contains("일 요청 건수");
    }

    private JsonNode node(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
