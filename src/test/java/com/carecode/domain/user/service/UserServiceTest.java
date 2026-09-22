package com.carecode.domain.user.service;

import com.carecode.core.analytics.EventLogger;
import com.carecode.core.exception.UserNotFoundException;
import com.carecode.domain.user.dto.request.PasswordChangeRequestDto;
import com.carecode.domain.user.dto.response.UserDto;
import com.carecode.domain.user.dto.response.UserStatsResponse;
import com.carecode.domain.user.entity.Gender;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.entity.UserRole;
import com.carecode.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserService 단위 테스트.
 *
 * <p>이 클래스는 인증·계정 상태·역할이 모인 곳인데 라인 커버리지가 0.7% 였다.
 * 그 사이에 여기서 P0 가 세 번 나왔다.
 * <ul>
 *   <li>#82 이메일 가입이 요청의 role 을 그대로 저장 → 누구나 ADMIN</li>
 *   <li>#83 updateUserRole 이 로그인만 하면 호출 가능한 경로에 노출</li>
 *   <li>카카오 가입 완료가 요청의 role 을 그대로 저장 → 누구나 ADMIN
 *       (JWT 필터가 이 경로를 건너뛰어 흐름 자체가 막혀 있던 덕에 드러나지 않았다)</li>
 * </ul>
 *
 * <p>가입 시 서버가 정하는 값은 {@link UserServiceSignUpTest} 에 있다. 여기서는 나머지를 다룬다.
 * {@code @PreAuthorize} 는 스프링 프록시가 적용하므로 단위 테스트 범위 밖이다 —
 * 그 계약은 접근제어 통합 테스트가 맡는다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserService")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RestTemplate restTemplate;
    @Mock private EventLogger eventLogger;

    @InjectMocks private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .userId("user_1")
                .email("me@example.com")
                .password("$2a$hash")
                .name("엄마")
                .gender(Gender.FEMALE)
                .birthDate(LocalDate.of(1990, 1, 1))
                .role(UserRole.PARENT)
                .isActive(true)
                .emailVerified(true)
                .registrationCompleted(true)
                .createdAt(LocalDateTime.now().minusDays(3))
                .build();

        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmailAndDeletedAtIsNull("me@example.com")).thenReturn(Optional.of(user));
    }

    private User savedUser() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        return captor.getValue();
    }

    // =====================================================================

    @Nested
    @DisplayName("카카오 가입 완료")
    class CompleteKakaoRegistration {

        private User kakaoNewbie;

        @BeforeEach
        void setUp() {
            kakaoNewbie = User.builder()
                    .id(2L).userId("user_k").email("kakao@example.com").name("카카오_1")
                    .role(UserRole.PARENT).provider("kakao").providerId("1")
                    .isActive(true).emailVerified(true).registrationCompleted(false)
                    .build();
            when(userRepository.findByEmailAndDeletedAtIsNull("kakao@example.com"))
                    .thenReturn(Optional.of(kakaoNewbie));
        }

        @ParameterizedTest(name = "{0} 로 가입을 끝낼 수 있다")
        @ValueSource(strings = {"PARENT", "CAREGIVER", "parent"})
        @DisplayName("스스로 고를 수 있는 역할이면 가입을 끝낸다")
        void acceptsSelfSelectableRoles(String role) {
            UserDto dto = userService.completeKakaoRegistration("kakao@example.com", "맘편한", role);

            User saved = savedUser();
            assertThat(saved.getRegistrationCompleted()).isTrue();
            assertThat(saved.getName()).isEqualTo("맘편한");
            assertThat(saved.getRole()).isEqualTo(UserRole.valueOf(role.toUpperCase()));
            assertThat(dto.getRegistrationCompleted()).isTrue();
        }

        @ParameterizedTest(name = "{0} 은 거부한다")
        @ValueSource(strings = {"ADMIN", "admin", "GUEST"})
        @DisplayName("관리자 등 스스로 고를 수 없는 역할은 거부하고 아무것도 저장하지 않는다")
        void rejectsPrivilegedRoles(String role) {
            assertThatThrownBy(() -> userService.completeKakaoRegistration("kakao@example.com", "맘편한", role))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("선택할 수 없는 역할");

            verify(userRepository, never()).save(any());
            assertThat(kakaoNewbie.getRole()).isEqualTo(UserRole.PARENT);
        }

        @Test
        @DisplayName("존재하지 않는 역할 문자열은 400 으로 끝난다")
        void rejectsUnknownRole() {
            assertThatThrownBy(() -> userService.completeKakaoRegistration("kakao@example.com", "맘편한", "SUPERUSER"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("유효하지 않은 역할");
        }

        @Test
        @DisplayName("이름·역할은 필수다")
        void requiresNameAndRole() {
            assertThatThrownBy(() -> userService.completeKakaoRegistration("kakao@example.com", " ", "PARENT"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> userService.completeKakaoRegistration("kakao@example.com", "맘편한", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("카카오 사용자가 아니면 거부한다")
        void rejectsNonKakaoUser() {
            assertThatThrownBy(() -> userService.completeKakaoRegistration("me@example.com", "맘편한", "PARENT"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("카카오 사용자가 아닙니다");
        }

        @Test
        @DisplayName("이미 가입을 끝낸 사용자는 다시 할 수 없다 — 역할을 바꾸는 우회로가 되면 안 된다")
        void rejectsAlreadyCompleted() {
            kakaoNewbie.setRegistrationCompleted(true);

            assertThatThrownBy(() -> userService.completeKakaoRegistration("kakao@example.com", "맘편한", "CAREGIVER"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이미 가입 완료");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("가입 완료 여부가 null 이어도 터지지 않는다")
        void toleratesNullRegistrationFlag() {
            // Boolean 이라 !user.getRegistrationCompleted() 는 null 에서 NPE 였다.
            kakaoNewbie.setRegistrationCompleted(null);

            userService.completeKakaoRegistration("kakao@example.com", "맘편한", "PARENT");

            assertThat(savedUser().getRegistrationCompleted()).isTrue();
        }

        @Test
        @DisplayName("사용자가 없으면 UserNotFound")
        void missingUser() {
            when(userRepository.findByEmailAndDeletedAtIsNull("nobody@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.completeKakaoRegistration("nobody@example.com", "맘편한", "PARENT"))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // =====================================================================

    @Nested
    @DisplayName("비밀번호 변경")
    class ChangePassword {

        @Test
        @DisplayName("현재 비밀번호가 맞으면 새 비밀번호를 해시해서 저장한다")
        void changesPassword() {
            when(passwordEncoder.matches("old", "$2a$hash")).thenReturn(true);
            when(passwordEncoder.encode("new-password")).thenReturn("$2a$new");

            userService.changePassword("1", new PasswordChangeRequestDto("old", "new-password"));

            assertThat(savedUser().getPassword()).isEqualTo("$2a$new");
        }

        @Test
        @DisplayName("현재 비밀번호가 틀리면 바꾸지 않는다")
        void rejectsWrongCurrentPassword() {
            when(passwordEncoder.matches("wrong", "$2a$hash")).thenReturn(false);

            assertThatThrownBy(() -> userService.changePassword("1", new PasswordChangeRequestDto("wrong", "new")))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(userRepository, never()).save(any());
        }
    }

    // =====================================================================

    @Nested
    @DisplayName("계정 상태")
    class AccountState {

        @Test
        @DisplayName("비활성화하면 isActive 가 false 가 된다")
        void deactivates() {
            userService.deactivateUser("1");
            assertThat(savedUser().getIsActive()).isFalse();
        }

        @Test
        @DisplayName("활성화하면 isActive 가 true 가 된다")
        void activates() {
            user.setIsActive(false);
            userService.activateUser("1");
            assertThat(savedUser().getIsActive()).isTrue();
        }

        @Test
        @DisplayName("숫자가 아닌 ID 는 400")
        void rejectsNonNumericId() {
            assertThatThrownBy(() -> userService.deactivateUser("abc")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> userService.activateUser("abc")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> userService.updateProfileImage("abc", "x")).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("없는 사용자는 UserNotFound")
        void missingUser() {
            when(userRepository.findById(404L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> userService.deactivateUser("404")).isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("소프트 삭제는 deletedAt 을 채우고 비활성화한다 (userId 로 찾는다)")
        void softDeletesByUserId() {
            when(userRepository.findByUserIdAndDeletedAtIsNull("user_1")).thenReturn(Optional.of(user));

            userService.deleteUser("user_1");

            User saved = savedUser();
            assertThat(saved.getDeletedAt()).isNotNull();
            assertThat(saved.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("소프트 삭제는 DB PK 로도 찾는다")
        void softDeletesByDatabaseId() {
            when(userRepository.findByUserIdAndDeletedAtIsNull("1")).thenReturn(Optional.empty());

            userService.deleteUser("1");

            assertThat(savedUser().getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("이미 삭제된 사용자는 다시 삭제하지 않는다")
        void refusesDoubleDelete() {
            user.setDeletedAt(LocalDateTime.now());
            when(userRepository.findByUserIdAndDeletedAtIsNull("1")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteUser("1")).isInstanceOf(UserNotFoundException.class);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("복구하면 deletedAt 을 비우고 활성화한다")
        void reactivates() {
            user.setIsActive(false);
            user.setDeletedAt(LocalDateTime.now());
            when(userRepository.findByUserId("user_1")).thenReturn(Optional.of(user));

            userService.reactivateUser("user_1");

            User saved = savedUser();
            assertThat(saved.getIsActive()).isTrue();
            assertThat(saved.getDeletedAt()).isNull();
        }

        @Test
        @DisplayName("이미 활성 계정은 복구 대상이 아니다")
        void refusesReactivatingActive() {
            when(userRepository.findByUserId("user_1")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.reactivateUser("user_1"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("isActive 가 null 이어도 복구가 터지지 않는다")
        void reactivateToleratesNullActiveFlag() {
            // 예전에는 user.getIsActive() 언박싱에서 NPE 였다.
            user.setIsActive(null);
            when(userRepository.findByUserId("user_1")).thenReturn(Optional.of(user));

            userService.reactivateUser("user_1");

            assertThat(savedUser().getIsActive()).isTrue();
        }
    }

    // =====================================================================

    @Nested
    @DisplayName("역할 변경 (관리자 전용)")
    class UpdateRole {

        @Test
        @DisplayName("대소문자와 무관하게 역할을 바꾼다")
        void updatesRole() {
            userService.updateUserRole(1L, "caregiver");
            assertThat(savedUser().getRole()).isEqualTo(UserRole.CAREGIVER);
        }

        @Test
        @DisplayName("없는 역할이면 400 이고 저장하지 않는다")
        void rejectsUnknownRole() {
            assertThatThrownBy(() -> userService.updateUserRole(1L, "ROOT"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("지원하지 않는 역할");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("없는 사용자는 UserNotFound")
        void missingUser() {
            when(userRepository.findById(404L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> userService.updateUserRole(404L, "ADMIN"))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // =====================================================================

    @Nested
    @DisplayName("프로필·위치")
    class ProfileAndLocation {

        @Test
        @DisplayName("프로필 이미지 URL 을 바꾼다")
        void updatesProfileImage() {
            userService.updateProfileImage("1", "/files/profile-images/a.png");
            assertThat(savedUser().getProfileImageUrl()).isEqualTo("/files/profile-images/a.png");
        }

        @Test
        @DisplayName("위치를 바꾸고 결과를 돌려준다")
        void updatesLocation() {
            userService.updateUserLocation("1", 37.5, 127.0);

            User saved = savedUser();
            assertThat(saved.getLatitude()).isEqualTo(37.5);
            assertThat(saved.getLongitude()).isEqualTo(127.0);
        }
    }

    // =====================================================================

    @Nested
    @DisplayName("조회")
    class Queries {

        @Test
        @DisplayName("userId 로 먼저 찾는다")
        void getsByUserId() {
            when(userRepository.findByUserIdAndDeletedAtIsNull("user_1")).thenReturn(Optional.of(user));
            assertThat(userService.getUserById("user_1").getEmail()).isEqualTo("me@example.com");
        }

        @Test
        @DisplayName("userId 로 없으면 DB PK 로 찾는다")
        void fallsBackToDatabaseId() {
            when(userRepository.findByUserIdAndDeletedAtIsNull("1")).thenReturn(Optional.empty());
            assertThat(userService.getUserById("1").getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("삭제된 사용자는 조회되지 않는다")
        void hidesDeletedUser() {
            user.setDeletedAt(LocalDateTime.now());
            when(userRepository.findByUserIdAndDeletedAtIsNull("1")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById("1")).isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("이메일로 조회한다")
        void getsByEmail() {
            assertThat(userService.getUserByEmail("me@example.com").getName()).isEqualTo("엄마");
            assertThat(userService.getUserEntityByEmail("me@example.com")).isSameAs(user);
            assertThat(userService.getUserByEmailOptional("me@example.com")).contains(user);
            assertThat(userService.findActiveUserEntityByEmail("me@example.com")).contains(user);
        }

        @Test
        @DisplayName("없는 이메일은 UserNotFound")
        void missingEmail() {
            when(userRepository.findByEmailAndDeletedAtIsNull("x@example.com")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> userService.getUserByEmail("x@example.com")).isInstanceOf(UserNotFoundException.class);
            assertThatThrownBy(() -> userService.getUserEntityByEmail("x@example.com")).isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("검색 타입은 name / email 만 받는다")
        void searchByType() {
            when(userRepository.findByNameContainingAndDeletedAtIsNull("엄")).thenReturn(List.of(user));
            when(userRepository.findByEmailContainingAndDeletedAtIsNull("me")).thenReturn(List.of(user));

            assertThat(userService.searchUsers("엄", "NAME")).hasSize(1);
            assertThat(userService.searchUsers("me", "email")).hasSize(1);
            assertThatThrownBy(() -> userService.searchUsers("x", "phone"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("통합 검색·목록 조회는 DTO 로 변환해 돌려준다")
        void listQueries() {
            when(userRepository.findByNameContainingOrEmailContainingAndDeletedAtIsNull("me", "me")).thenReturn(List.of(user));
            when(userRepository.findByIsActiveTrue()).thenReturn(List.of(user));
            when(userRepository.findByRole("PARENT")).thenReturn(List.of(user));
            when(userRepository.findByAddressContaining("서울")).thenReturn(List.of(user));
            when(userRepository.findByEmailVerifiedTrue()).thenReturn(List.of(user));
            when(userRepository.findByUpdatedAtAfter(any())).thenReturn(List.of(user));
            when(userRepository.findByRoleAndDeletedAtIsNull("PARENT")).thenReturn(List.of(user));
            when(userRepository.findByAddressContainingAndDeletedAtIsNull("서울")).thenReturn(List.of(user));
            when(userRepository.findEmailVerifiedUsersNotDeleted()).thenReturn(List.of(user));

            assertThat(userService.searchUsers("me")).hasSize(1);
            assertThat(userService.getActiveUsers()).hasSize(1);
            assertThat(userService.getUsersByType("PARENT")).hasSize(1);
            assertThat(userService.getUsersByRegion("서울")).hasSize(1);
            assertThat(userService.getVerifiedUsers()).hasSize(1);
            assertThat(userService.getRecentlyActiveUsers()).hasSize(1);
            assertThat(userService.getUsersByRole("PARENT")).hasSize(1);
            assertThat(userService.getUsersByLocation("서울")).hasSize(1);
            assertThat(userService.getEmailVerifiedUsers()).hasSize(1);
        }

        @Test
        @DisplayName("통계는 저장소의 집계를 그대로 담는다")
        void statistics() {
            when(userRepository.count()).thenReturn(10L);
            when(userRepository.countActiveUsersNotDeleted()).thenReturn(8L);
            when(userRepository.countEmailVerifiedUsersNotDeleted()).thenReturn(6L);
            when(userRepository.countNewUsersSince(any())).thenReturn(2L);

            UserStatsResponse stats = userService.getUserStatistics();

            assertThat(stats.getTotalUsers()).isEqualTo(10L);
            assertThat(stats.getActiveUsers()).isEqualTo(8L);
            assertThat(stats.getVerifiedUsers()).isEqualTo(6L);
            assertThat(stats.getNewUsersToday()).isEqualTo(2L);
        }
    }

    // =====================================================================

    @Nested
    @DisplayName("DTO 변환")
    class Conversion {

        @Test
        @DisplayName("응답에 비밀번호 해시가 실리지 않는다")
        void doesNotExposePasswordHash() {
            // UserDto.password 는 WRITE_ONLY 다. 변환 단계에서도 아예 채우지 않는다.
            assertThat(userService.convertToDto(user).getPassword()).isNull();
        }

        @Test
        @DisplayName("성별·역할을 문자열로 옮긴다")
        void mapsEnums() {
            UserDto dto = userService.convertToDto(user);
            assertThat(dto.getGender()).isEqualTo("FEMALE");
            assertThat(dto.getRole()).isEqualTo("PARENT");
        }

        @Test
        @DisplayName("성별이 없으면 null 이다")
        void nullGender() {
            user.setGender(null);
            assertThat(userService.convertToDto(user).getGender()).isNull();
        }
    }

    // =====================================================================

    @Nested
    @DisplayName("카카오 사용자 정보 조회")
    class KakaoUserInfo {

        @SuppressWarnings("unchecked")
        private void kakaoReturns(Map<String, Object> body) {
            when(restTemplate.exchange(eq("https://kapi.kakao.com/v2/user/me"), eq(HttpMethod.GET),
                    any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                    .thenReturn(ResponseEntity.ok(body));
        }

        @Test
        @DisplayName("id 와 프로필을 뽑아낸다")
        void extractsProfile() {
            kakaoReturns(Map.of(
                    "id", 12345L,
                    "kakao_account", Map.of("profile", Map.of(
                            "nickname", "맘편한", "profile_image_url", "https://k.kakao.com/p.png"))));

            Map<String, Object> info = userService.getKakaoUserInfo("kakao-access-token");

            assertThat(info).containsEntry("id", 12345L)
                    .containsEntry("nickname", "맘편한")
                    .containsEntry("profileImageUrl", "https://k.kakao.com/p.png");
        }

        @Test
        @DisplayName("id 가 없으면 실패한다")
        void requiresId() {
            kakaoReturns(Map.of("kakao_account", Map.of()));

            assertThatThrownBy(() -> userService.getKakaoUserInfo("t")).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("카카오가 401 을 주면 토큰 문제로 알린다")
        void unauthorizedToken() {
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
                    any(ParameterizedTypeReference.class)))
                    .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

            assertThatThrownBy(() -> userService.getKakaoUserInfo("expired"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("유효하지 않습니다");
        }
    }
}
