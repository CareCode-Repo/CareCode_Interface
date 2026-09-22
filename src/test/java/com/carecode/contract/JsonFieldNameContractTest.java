package com.carecode.contract;

import com.carecode.domain.community.dto.request.CommunityCreateCommentRequest;
import com.carecode.domain.community.dto.request.CommunityCreatePostRequest;
import com.carecode.domain.health.dto.response.GrowthPointResponse;
import com.carecode.domain.notification.dto.response.NotificationInfoResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lombok 이 primitive {@code boolean isX} 의 접근자를 {@code isX()}/{@code setX()} 로 만들면
 * Jackson 은 JSON 키를 {@code x} 로 쓴다. 프런트는 {@code isX} 를 주고받으므로 값이 조용히 사라진다.
 * {@code zScore} 도 {@code getZScore()} 때문에 {@code zscore} 가 된다. 키 이름을 여기서 고정한다.
 */
@DisplayName("JSON 필드 이름 계약")
class JsonFieldNameContractTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("게시글·댓글 작성 요청은 isAnonymous 를 읽는다 (예전 키 anonymous 도 받는다)")
    void anonymousFlagIsRead() throws Exception {
        assertThat(mapper.readValue("{\"title\":\"t\",\"content\":\"c\",\"isAnonymous\":true}",
                CommunityCreatePostRequest.class).isAnonymous()).isTrue();
        assertThat(mapper.readValue("{\"content\":\"c\",\"isAnonymous\":true}",
                CommunityCreateCommentRequest.class).isAnonymous()).isTrue();
        assertThat(mapper.readValue("{\"title\":\"t\",\"content\":\"c\",\"anonymous\":true}",
                CommunityCreatePostRequest.class).isAnonymous()).isTrue();
    }

    @Test
    @DisplayName("알림 응답은 isRead 키 하나로 나간다")
    void notificationReadFlag() throws Exception {
        JsonNode json = mapper.valueToTree(NotificationInfoResponse.builder().isRead(true).build());
        assertThat(json.path("isRead").asBoolean()).isTrue();
        assertThat(json.has("read")).isFalse();
    }

    @Test
    @DisplayName("성장 곡선 응답은 zScore 키로 나간다")
    void growthZScore() throws Exception {
        JsonNode json = mapper.valueToTree(GrowthPointResponse.builder().zScore(1.5).build());
        assertThat(json.path("zScore").asDouble()).isEqualTo(1.5);
        assertThat(json.has("zscore")).isFalse();
    }
}
