package com.carecode.domain.user.service;

import com.carecode.core.exception.ChildNotFoundException;
import com.carecode.domain.user.app.ChildView;
import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.ChildRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("자녀 조회 입구")
class ChildDirectoryServiceTest {

    @Mock ChildRepository childRepository;

    private ChildDirectoryService directory;

    @BeforeEach
    void setUp() {
        directory = new ChildDirectoryService(childRepository);
    }

    private User user(long id) {
        return User.builder().id(id).build();
    }

    private Child child(long id, long userId, LocalDate birthDate) {
        return Child.builder()
                .id(id)
                .user(user(userId))
                .name("아이" + id)
                .birthDate(birthDate)
                .build();
    }

    @Test
    @DisplayName("사용자의 자녀를 읽기 전용 값으로 돌려준다")
    void returnsViewsNotEntities() {
        when(childRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(child(10L, 1L, LocalDate.of(2024, 3, 1))));

        List<ChildView> children = directory.childrenOf(1L);

        assertThat(children).hasSize(1);
        assertThat(children.get(0).childId()).isEqualTo(10L);
        assertThat(children.get(0).birthDate()).isEqualTo(LocalDate.of(2024, 3, 1));
    }

    /**
     * 남의 자녀와 없는 자녀를 구분해서 알려주면, ID 를 훑어 "이 번호는 누군가의 자녀" 라는
     * 사실을 알아낼 수 있다. 둘 다 같은 예외로 끝나야 한다.
     */
    @Test
    @DisplayName("남의 자녀도 없는 자녀와 똑같이 '찾을 수 없다' 로 끝난다")
    void otherUsersChildIsIndistinguishableFromMissing() {
        when(childRepository.findById(10L)).thenReturn(Optional.of(child(10L, 1L, null)));
        when(childRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> directory.requireOwnedChild(10L, 2L))
                .as("다른 사용자의 자녀")
                .isInstanceOf(ChildNotFoundException.class);

        assertThatThrownBy(() -> directory.requireOwnedChild(99L, 2L))
                .as("아예 없는 자녀")
                .isInstanceOf(ChildNotFoundException.class);
    }

    @Test
    @DisplayName("본인 자녀는 통과한다")
    void ownChildPasses() {
        when(childRepository.findById(10L)).thenReturn(Optional.of(child(10L, 1L, null)));

        assertThat(directory.requireOwnedChild(10L, 1L).childId()).isEqualTo(10L);
    }

    /** 로그인 주체를 못 꺼냈을 때(null) 통과시키면 모든 자녀가 열린다. */
    @Test
    @DisplayName("요청자가 없으면 통과시키지 않는다")
    void nullActorNeverPasses() {
        when(childRepository.findById(10L)).thenReturn(Optional.of(child(10L, 1L, null)));

        assertThatThrownBy(() -> directory.requireOwnedChild(10L, null))
                .isInstanceOf(ChildNotFoundException.class);
    }

    @Test
    @DisplayName("사용자가 없으면 DB 를 보지 않고 빈 목록을 준다")
    void nullUserIdSkipsQuery() {
        assertThat(directory.childrenOf(null)).isEmpty();
    }

    /**
     * 월령 계산이 정책 추천의 입력이다. 생일이 없을 때 0 을 주면 "갓 태어난 아이" 로 읽혀
     * 신생아 정책이 추천된다. 없는 값은 없는 값으로 둬야 한다.
     */
    @Test
    @DisplayName("생일이 없으면 월령도 없다 — 0개월로 바꾸지 않는다")
    void missingBirthDateHasNoAge() {
        ChildView withoutBirthDate = ChildView.from(child(10L, 1L, null));
        assertThat(withoutBirthDate.ageMonths(LocalDate.of(2026, 10, 5))).isNull();

        ChildView born = ChildView.from(child(11L, 1L, LocalDate.of(2025, 4, 5)));
        assertThat(born.ageMonths(LocalDate.of(2026, 10, 5))).isEqualTo(18);
    }

    @Test
    @DisplayName("특별 고려사항은 빈 문자열과 없음을 같게 본다")
    void blankSpecialNeedsIsNone() {
        Child blank = Child.builder().id(1L).user(user(1L)).specialNeeds("   ").build();
        Child filled = Child.builder().id(2L).user(user(1L)).specialNeeds("알레르기").build();

        assertThat(ChildView.from(blank).hasSpecialNeeds()).isFalse();
        assertThat(ChildView.from(filled).hasSpecialNeeds()).isTrue();
    }
}
