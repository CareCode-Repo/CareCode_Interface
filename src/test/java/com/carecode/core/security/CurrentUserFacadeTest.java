package com.carecode.core.security;

import com.carecode.core.exception.CareServiceException;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 본인 확인(소유권) 검증에 대한 테스트.
 *
 * <p>{@code /users/{userId}/...} 계열은 경로에 남의 식별자를 넣어도 그대로 동작했다.
 * 남의 위치를 바꾸고, 남의 계정을 탈퇴시키고, 남의 프로필 이미지를 갈아끼울 수 있었다.
 * 그 경로들이 다시 열리지 않도록 여기서 계약을 고정한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CurrentUserFacade - 본인 확인")
class CurrentUserFacadeTest {

    private static final String ME_EMAIL = "me@example.com";

    @Mock private UserRepository userRepository;

    @InjectMocks private CurrentUserFacade currentUserFacade;

    private User me;

    @BeforeEach
    void setUp() {
        me = User.builder()
                .id(1L)
                .userId("user_me")
                .email(ME_EMAIL)
                .name("나")
                .build();

        when(userRepository.findByEmailAndDeletedAtIsNull(ME_EMAIL)).thenReturn(Optional.of(me));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String email, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        email, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Nested
    @DisplayName("requireSelf")
    class RequireSelf {

        @Test
        @DisplayName("발급된 userId 로 본인을 지목하면 통과한다")
        void acceptsOwnUserId() {
            loginAs(ME_EMAIL, "PARENT");

            assertThat(currentUserFacade.requireSelf("user_me")).isSameAs(me);
        }

        @Test
        @DisplayName("DB PK 로 본인을 지목해도 통과한다")
        void acceptsOwnDatabaseId() {
            loginAs(ME_EMAIL, "PARENT");

            // 서비스 계층이 두 형태를 모두 받아 조회하므로, 검증도 두 형태를 모두 인정해야 한다.
            assertThat(currentUserFacade.requireSelf("1")).isSameAs(me);
        }

        @Test
        @DisplayName("남의 userId 를 넣으면 403 으로 막힌다")
        void rejectsOtherUserId() {
            loginAs(ME_EMAIL, "PARENT");

            assertThatThrownBy(() -> currentUserFacade.requireSelf("user_someone_else"))
                    .isInstanceOf(CareServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "FORBIDDEN");
        }

        @Test
        @DisplayName("남의 DB PK 를 넣어도 막힌다")
        void rejectsOtherDatabaseId() {
            loginAs(ME_EMAIL, "PARENT");

            assertThatThrownBy(() -> currentUserFacade.requireSelf("2"))
                    .isInstanceOf(CareServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "FORBIDDEN");
        }

        @Test
        @DisplayName("관리자여도 requireSelf 로는 남을 건드릴 수 없다")
        void adminIsNotExemptFromRequireSelf() {
            // 관리 기능은 /api/admin/** 로 분리했다. 본인 전용 경로에 관리자 우회를 두면
            // 그 경로가 다시 관리 API 처럼 쓰이기 시작한다.
            loginAs(ME_EMAIL, "ADMIN");

            assertThatThrownBy(() -> currentUserFacade.requireSelf("999"))
                    .isInstanceOf(CareServiceException.class);
        }

        @Test
        @DisplayName("빈 식별자는 본인으로 인정하지 않는다")
        void rejectsBlankId() {
            loginAs(ME_EMAIL, "PARENT");

            assertThatThrownBy(() -> currentUserFacade.requireSelf(""))
                    .isInstanceOf(CareServiceException.class);
            assertThatThrownBy(() -> currentUserFacade.requireSelf(null))
                    .isInstanceOf(CareServiceException.class);
        }

        @Test
        @DisplayName("비로그인 상태면 401 로 막힌다")
        void rejectsAnonymous() {
            SecurityContextHolder.getContext().setAuthentication(
                    new AnonymousAuthenticationToken("key", "anonymousUser",
                            List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

            assertThatThrownBy(() -> currentUserFacade.requireSelf("1"))
                    .isInstanceOf(CareServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "UNAUTHORIZED");
        }
    }

    @Nested
    @DisplayName("requireSelfOrAdmin")
    class RequireSelfOrAdmin {

        @Test
        @DisplayName("관리자는 남의 식별자로도 통과한다")
        void adminPasses() {
            loginAs(ME_EMAIL, "ADMIN");

            assertThat(currentUserFacade.requireSelfOrAdmin("999")).isSameAs(me);
        }

        @Test
        @DisplayName("일반 사용자는 본인이 아니면 막힌다")
        void nonAdminBlocked() {
            loginAs(ME_EMAIL, "PARENT");

            assertThatThrownBy(() -> currentUserFacade.requireSelfOrAdmin("999"))
                    .isInstanceOf(CareServiceException.class);
        }
    }

    @Nested
    @DisplayName("isAdmin")
    class IsAdmin {

        @Test
        @DisplayName("ROLE_ADMIN 권한이 있을 때만 참이다")
        void detectsAdminRole() {
            loginAs(ME_EMAIL, "ADMIN");
            assertThat(currentUserFacade.isAdmin()).isTrue();

            SecurityContextHolder.clearContext();
            loginAs(ME_EMAIL, "PARENT");
            assertThat(currentUserFacade.isAdmin()).isFalse();
        }
    }
}
