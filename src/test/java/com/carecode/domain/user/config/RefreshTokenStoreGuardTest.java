package com.carecode.domain.user.config;

import com.carecode.domain.user.service.refreshtoken.NoopRefreshTokenStore;
import com.carecode.domain.user.service.refreshtoken.RefreshTokenStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 운영에서 리프레시 토큰 저장소 없이 뜨지 못하게 한다.
 *
 * <p>{@link NoopRefreshTokenStore} 가 운영에 올라가면 로그아웃이 <b>아무것도 폐기하지
 * 못한다.</b> 응답은 200 이고 클라이언트는 로그아웃된 것으로 보이지만, 탈취된 리프레시 토큰은
 * 만료(기본 30일) 전까지 계속 새 액세스 토큰을 받아갈 수 있다. 조용히 틀린 상태라 운영에서
 * 알아차릴 방법이 없다.
 *
 * <p>운영 프로파일의 기본값은 {@code redis} 지만 {@code JWT_REFRESH_TOKEN_STORE} 로 덮을 수
 * 있어서, 기본값에 기대지 않고 막는다. 이슈 #90
 */
@DisplayName("리프레시 토큰 저장소 전제")
class RefreshTokenStoreGuardTest {

    private final RefreshTokenStoreConfiguration configuration = new RefreshTokenStoreConfiguration();

    @Test
    @DisplayName("운영에서 저장소가 없으면 기동하지 않는다")
    void prodRefusesToStartWithoutStore() {
        MockEnvironment prod = new MockEnvironment();
        prod.setActiveProfiles("prod");

        assertThatThrownBy(() -> configuration.noopRefreshTokenStore(prod))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("리프레시 토큰 저장소");
    }

    /** 로컬·CI 는 Redis 없이 떠야 한다. 막는 범위가 넓으면 개발이 멈춘다. */
    @Test
    @DisplayName("로컬·CI 는 저장소 없이 뜬다")
    void nonProdStartsWithoutStore() {
        MockEnvironment dev = new MockEnvironment();
        dev.setActiveProfiles("dev");

        RefreshTokenStore store = configuration.noopRefreshTokenStore(dev);

        assertThat(store).isInstanceOf(NoopRefreshTokenStore.class);
    }
}
