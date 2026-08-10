package com.carecode.domain.health.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 표준 예방접종 일정 정의 검증. */
@DisplayName("예방접종 표준 일정")
class VaccineTypeTest {

    @Test
    @DisplayName("모든 백신은 최소 1회차 이상 정의되어 있다")
    void everyVaccineHasAtLeastOneDose() {
        for (VaccineType vaccine : VaccineType.all()) {
            assertThat(vaccine.getTotalDoses())
                    .as("%s 의 회차 수", vaccine.name())
                    .isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    @DisplayName("회차가 늘어날수록 접종 시기도 늦어진다")
    void doseMonthsAreNonDecreasing() {
        for (VaccineType vaccine : VaccineType.all()) {
            int previous = -1;
            for (int dose = 1; dose <= vaccine.getTotalDoses(); dose++) {
                int months = vaccine.getMonthsForDose(dose);
                assertThat(months)
                        .as("%s %d차 접종 시기", vaccine.name(), dose)
                        .isGreaterThanOrEqualTo(previous);
                previous = months;
            }
        }
    }

    @Test
    @DisplayName("생년월일에 개월 수를 더해 접종 예정일을 계산한다")
    void calculatesDueDateFromBirthDate() {
        LocalDate birthDate = LocalDate.of(2026, 1, 15);

        // DTaP 1차는 생후 2개월
        int months = VaccineType.DTAP.getMonthsForDose(1);
        assertThat(months).isEqualTo(2);
        assertThat(birthDate.plusMonths(months)).isEqualTo(LocalDate.of(2026, 3, 15));
    }

    @Test
    @DisplayName("존재하지 않는 회차는 거부한다")
    void rejectsInvalidDoseNumber() {
        assertThatThrownBy(() -> VaccineType.BCG.getMonthsForDose(2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VaccineType.BCG.getMonthsForDose(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("아이 한 명당 생성되는 접종 일정은 20건을 넘는다")
    void generatesMeaningfulScheduleCount() {
        int total = VaccineType.all().stream().mapToInt(VaccineType::getTotalDoses).sum();

        assertThat(total).isGreaterThan(20);
    }
}
