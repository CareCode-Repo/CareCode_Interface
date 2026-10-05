package com.carecode.contract;

import com.carecode.domain.admin.dto.response.AdminBookingDashboardResponse;
import com.carecode.domain.admin.dto.response.AdminDashboardResponse;
import com.carecode.domain.admin.dto.response.DeadlineNotifyResponse;
import com.carecode.domain.admin.dto.response.ForecastAccuracyMeasureResponse;
import com.carecode.domain.admin.dto.response.GeocodingResultResponse;
import com.carecode.domain.admin.dto.response.PolicyVerificationResponse;
import com.carecode.domain.admin.dto.response.RegionVerificationStatusResponse;
import com.carecode.domain.admin.dto.response.SyncResultResponse;
import com.carecode.domain.admin.dto.response.SyncStatusResponse;
import com.carecode.domain.admin.dto.response.VacancyNotifyResponse;
import com.carecode.domain.community.dto.request.CommunityCreateCommentRequest;
import com.carecode.domain.community.dto.request.CommunityCreatePostRequest;
import com.carecode.domain.community.dto.response.PostBookmarkCountResponse;
import com.carecode.domain.community.dto.response.PostBookmarkToggleResponse;
import com.carecode.domain.community.dto.response.PostLikeCountResponse;
import com.carecode.domain.community.dto.response.PostLikeToggleResponse;
import com.carecode.domain.facility.dto.response.WaitlistEntryResponse;
import com.carecode.domain.facility.dto.response.WaitlistRegisterResponse;
import com.carecode.domain.health.dto.response.GrowthPointResponse;
import com.carecode.domain.health.dto.response.HealthRecommendationResponse;
import com.carecode.domain.notification.dto.response.NotificationInfoResponse;
import com.carecode.domain.user.dto.response.KakaoLoginUrlResponse;
import com.carecode.domain.user.dto.response.MyDataExportResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

    /**
     * 좋아요·북마크 토글의 키를 고정한다.
     *
     * <p>레코드는 컴포넌트 이름을 그대로 키로 쓰므로 {@code isLiked} 는 그대로 나간다. 위험한
     * 쪽은 **클래스로 바꿀 때**다. Lombok {@code @Getter} 의 {@code isLiked()} 는 Jackson 이
     * {@code is} 를 떼어 {@code liked} 로 만든다(이 파일 위쪽의 {@code isRead}·{@code isAnonymous}
     * 가 같은 사고였다). 프런트는 {@code isLiked} 를 {@code z.boolean()} 으로 필수로 읽으므로
     * 그 순간 토글이 파싱 단계에서 깨진다. 값이 아니라 키가 틀리는 종류라 눈으로는 안 보인다.
     */
    @Test
    @DisplayName("좋아요·북마크 토글 응답은 isLiked·isBookmarked 키로 나간다")
    void toggleFlagsKeepIsPrefix() throws Exception {
        JsonNode like = mapper.valueToTree(new PostLikeToggleResponse(true, 7L));
        assertThat(like.path("isLiked").asBoolean()).isTrue();
        assertThat(like.path("likeCount").asLong()).isEqualTo(7L);
        assertThat(like.has("liked")).as("is 접두사가 떨어지면 프런트가 못 읽는다").isFalse();


        JsonNode bookmark = mapper.valueToTree(new PostBookmarkToggleResponse(true, 3L));
        assertThat(bookmark.path("isBookmarked").asBoolean()).isTrue();
        assertThat(bookmark.path("bookmarkCount").asLong()).isEqualTo(3L);
        assertThat(bookmark.has("bookmarked")).isFalse();
    }

    /**
     * {@code Map} 응답을 DTO 로 바꾼 자리들. 바꾸면서 키가 하나라도 달라지면 프런트가 그 값을
     * 조용히 {@code undefined} 로 읽는다(zod 가 {@code nullish} 로 받는 키가 많아 예외도 안 난다).
     * 그래서 **키 집합을 그대로** 적어 둔다.
     */
    @Test
    @DisplayName("Map 에서 DTO 로 바꾼 응답의 키 집합이 그대로다")
    void typedResponsesKeepTheirKeys() {
        assertKeys(new WaitlistRegisterResponse(1L), "waitlistId");

        assertKeys(new WaitlistEntryResponse(1L, 2L, 3, LocalDate.now(), "WAITING", "대기 중", 10L),
                "waitlistId", "facilityId", "waitNumber", "appliedAt", "status", "statusName", "waitedDays");

        assertKeys(new PostLikeCountResponse(1L), "likeCount");
        assertKeys(new PostBookmarkCountResponse(1L), "bookmarkCount");

        assertKeys(KakaoLoginUrlResponse.of("https://kauth.kakao.com/oauth/authorize"),
                "success", "loginUrl", "message");

        assertKeys(new HealthRecommendationResponse("u1", 3, List.of(), List.of(), "안내"),
                "userId", "childAge", "recommendedPolicies", "recommendedFacilities", "nudgeMessage");

        assertKeys(new SyncResultResponse("p", "r", true, 1, 2, 3, 4, 5, null),
                "provider", "resource", "completed", "created", "updated", "failed", "skipped",
                "pagesProcessed", "stoppedReason");

        assertKeys(new GeocodingResultResponse(1, 2, 3L, null),
                "resolved", "failed", "remaining", "skippedReason");

        assertKeys(new VacancyNotifyResponse(1, 2, 3),
                "facilitiesChecked", "facilitiesWithVacancy", "notificationsSent");

        assertKeys(new DeadlineNotifyResponse(1, 2), "policiesDueSoon", "notificationsSent");

        assertKeys(SyncStatusResponse.of(List.of()), "jobs", "staleCount");

        assertKeys(ForecastAccuracyMeasureResponse.of(List.of()), "measured", "results");

        assertKeys(new PolicyVerificationResponse(1L, "t", LocalDateTime.now(), "admin@example.com"),
                "policyId", "title", "verifiedAt", "verifiedBy");

        assertKeys(new RegionVerificationStatusResponse("서울", 10, 4, 6, 40),
                "region", "total", "verified", "unverified", "verifiedRate");

        assertKeys(new AdminDashboardResponse(1, 2, 3, List.of(), List.of(), List.of()),
                "userCount", "hospitalCount", "policyCount", "recentActivities",
                "userTrendLabels", "userTrendData");

        assertKeys(new AdminDashboardResponse.RecentActivity("user", "가입", "2026-10-05 01:00"),
                "type", "desc", "time");

        assertKeys(new AdminBookingDashboardResponse(null, List.of(), List.of()),
                "stats", "recentBookings", "todayBookings");

        assertKeys(new MyDataExportResponse(LocalDateTime.now(), null, List.of(), 0, 0L, List.of()),
                "exportedAt", "profile", "children", "healthRecordCount", "postCount", "consentHistory");

        assertKeys(new MyDataExportResponse.ChildData("아이", "2024-01-01", "FEMALE"),
                "name", "birthDate", "gender");
    }

    /** 키가 빠졌는지와 **새 키가 늘었는지**를 함께 본다. 늘어난 키도 계약 변경이다. */
    private void assertKeys(Object response, String... expected) {
        JsonNode json = mapper.valueToTree(response);
        assertThat(json.fieldNames())
                .as("%s 의 JSON 키", response.getClass().getSimpleName())
                .toIterable()
                .containsExactlyInAnyOrder(expected);
    }
}
