package com.carecode.domain.health.service;

import com.carecode.core.exception.ResourceNotFoundException;
import com.carecode.domain.health.entity.VaccinationSchedule;
import com.carecode.domain.health.entity.VaccineType;
import com.carecode.domain.health.repository.VaccinationScheduleRepository;
import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.repository.ChildRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 컨트롤러는 "경로의 아이가 내 아이인가" 만 확인한다. 일정이 그 아이 것인지 서비스가 보지 않으면
 * 내 아이 ID 에 남의 일정 ID 를 붙여 남의 접종 기록을 완료로 바꿀 수 있었다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("접종 완료 처리 — 일정 소유 확인")
class VaccinationScheduleServiceOwnershipTest {

    @Mock VaccinationScheduleRepository scheduleRepository;
    @Mock ChildRepository childRepository;
    @InjectMocks VaccinationScheduleService service;

    @Test
    @DisplayName("다른 아이의 일정이면 404 이고 저장하지 않는다")
    void rejectsScheduleOfAnotherChild() {
        VaccinationSchedule othersSchedule = schedule(99L);
        when(scheduleRepository.findById(5L)).thenReturn(Optional.of(othersSchedule));

        assertThatThrownBy(() -> service.markCompleted(1L, 5L, LocalDate.now()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(scheduleRepository, never()).save(any());
        assertThat(othersSchedule.getCompletedDate()).isNull();
    }

    @Test
    @DisplayName("같은 아이의 일정이면 완료 처리된다")
    void completesOwnSchedule() {
        VaccinationSchedule mine = schedule(1L);
        when(scheduleRepository.findById(5L)).thenReturn(Optional.of(mine));
        when(scheduleRepository.save(mine)).thenReturn(mine);

        service.markCompleted(1L, 5L, LocalDate.of(2026, 1, 2));

        assertThat(mine.getCompletedDate()).isEqualTo(LocalDate.of(2026, 1, 2));
    }

    private static VaccinationSchedule schedule(Long childId) {
        return VaccinationSchedule.builder()
                .id(5L)
                .child(Child.builder().id(childId).build())
                .vaccineType(VaccineType.HEP_B)
                .doseNumber(1)
                .dueDate(LocalDate.now())
                .build();
    }
}
