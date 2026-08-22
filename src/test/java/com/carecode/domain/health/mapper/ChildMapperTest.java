package com.carecode.domain.health.mapper;

import com.carecode.domain.health.dto.response.ChildInfoResponse;
import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 아이 정보 응답에 특이사항이 실리는지 고정한다.
 *
 * <p>응답 DTO 에 {@code specialNeeds} 가 빠져 있었다. 등록·수정 요청
 * ({@code ChildCreateRequest})은 이 값을 받는데 응답에는 없어서,
 * 수정 화면이 현재 값을 읽어올 방법이 없었다.
 *
 * <p>{@code ChildService.updateChild} 는 전체 교체다.
 *
 * <pre>
 * child.setSpecialNeeds(request.getSpecialNeeds());   // 조건 없이 덮어쓴다
 * </pre>
 *
 * <p>즉 클라이언트가 이름만 바꿔 보내면 특이사항이 {@code null} 로 지워진다.
 * 알레르기·기저질환처럼 지워지면 곤란한 값이라 응답에 반드시 있어야 한다.
 */
@DisplayName("ChildMapper - 응답 변환")
class ChildMapperTest {

    private final ChildMapper mapper = new ChildMapper();

    private Child child(String specialNeeds) {
        return Child.builder()
                .id(1L)
                .user(User.builder().id(10L).userId("user_a").build())
                .name("아이")
                .gender("FEMALE")
                .birthDate(LocalDate.of(2024, 3, 1))
                .specialNeeds(specialNeeds)
                .createdAt(LocalDateTime.of(2026, 1, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 2, 1, 9, 0))
                .build();
    }

    @Test
    @DisplayName("특이사항이 응답에 포함된다")
    void includesSpecialNeeds() {
        ChildInfoResponse response = mapper.toResponse(child("우유 알레르기"));

        // 이 값이 응답에 없으면 수정 요청이 현재 값을 되돌려줄 수 없어 그대로 유실된다.
        assertThat(response.getSpecialNeeds()).isEqualTo("우유 알레르기");
    }

    @Test
    @DisplayName("특이사항이 없으면 null 로 내려간다")
    void handlesMissingSpecialNeeds() {
        assertThat(mapper.toResponse(child(null)).getSpecialNeeds()).isNull();
    }

    @Test
    @DisplayName("나머지 필드도 그대로 옮긴다")
    void mapsRemainingFields() {
        ChildInfoResponse response = mapper.toResponse(child("천식"));

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUserId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("아이");
        assertThat(response.getGender()).isEqualTo("FEMALE");
        assertThat(response.getBirthDate()).isEqualTo("2024-03-01");
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("수정된 적이 없으면 updatedAt 은 null 이다")
    void nullUpdatedAtIsTolerated() {
        Child neverUpdated = child("천식");
        neverUpdated.setUpdatedAt(null);

        assertThat(mapper.toResponse(neverUpdated).getUpdatedAt()).isNull();
    }
}
