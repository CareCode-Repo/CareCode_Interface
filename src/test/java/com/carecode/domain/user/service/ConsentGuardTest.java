package com.carecode.domain.user.service;

import com.carecode.domain.user.entity.ConsentType;
import com.carecode.domain.user.entity.UserConsent;
import com.carecode.domain.user.repository.UserConsentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("민감정보 동의 확인")
class ConsentGuardTest {

    private UserConsentRepository repository;
    private ConsentGuard guard;

    @BeforeEach
    void setUp() {
        repository = mock(UserConsentRepository.class);
        guard = new ConsentGuard(repository);
    }

    @Test
    @DisplayName("동의 이력이 없으면 막는다")
    void blocksWithoutConsent() {
        when(repository.findLatest(anyLong(), eq(ConsentType.HEALTH_DATA))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.require(1L, ConsentType.HEALTH_DATA))
                .isInstanceOf(ConsentGuard.ConsentRequiredException.class)
                .hasMessageContaining("건강정보");
    }

    @Test
    @DisplayName("철회된 동의는 없는 것으로 본다")
    void blocksWhenRevoked() {
        // when() 안에서 mock 을 만들면 스터빙이 중첩돼 Mockito 가 거부한다
        UserConsent revoked = consent(false);
        when(repository.findLatest(anyLong(), eq(ConsentType.HEALTH_DATA)))
                .thenReturn(Optional.of(revoked));

        assertThat(guard.hasConsent(1L, ConsentType.HEALTH_DATA)).isFalse();
        assertThatThrownBy(() -> guard.require(1L, ConsentType.HEALTH_DATA))
                .isInstanceOf(ConsentGuard.ConsentRequiredException.class);
    }

    @Test
    @DisplayName("동의했으면 통과시킨다")
    void allowsWhenGranted() {
        UserConsent granted = consent(true);
        when(repository.findLatest(anyLong(), eq(ConsentType.HEALTH_DATA)))
                .thenReturn(Optional.of(granted));

        assertThat(guard.hasConsent(1L, ConsentType.HEALTH_DATA)).isTrue();
        guard.require(1L, ConsentType.HEALTH_DATA); // 예외 없음
    }

    @Test
    @DisplayName("어떤 동의가 필요한지 예외에 담아 알린다")
    void exposesRequiredConsentType() {
        when(repository.findLatest(anyLong(), eq(ConsentType.HEALTH_DATA))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.require(1L, ConsentType.HEALTH_DATA))
                .isInstanceOfSatisfying(ConsentGuard.ConsentRequiredException.class,
                        e -> assertThat(e.getConsentType()).isEqualTo(ConsentType.HEALTH_DATA));
    }

    @Test
    @DisplayName("건강정보는 민감정보로 분류된다")
    void healthDataIsSensitive() {
        assertThat(ConsentType.HEALTH_DATA.isSensitive()).isTrue();
        assertThat(ConsentType.PRIVACY_POLICY.isSensitive()).isFalse();
        // 일반 개인정보 동의로 갈음할 수 없어야 한다
        assertThat(ConsentType.HEALTH_DATA).isNotEqualTo(ConsentType.PRIVACY_POLICY);
    }

    private UserConsent consent(boolean granted) {
        UserConsent c = mock(UserConsent.class);
        when(c.isGranted()).thenReturn(granted);
        return c;
    }
}
