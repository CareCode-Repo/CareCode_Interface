package com.carecode.domain.health.dto.response;

import com.carecode.domain.health.entity.VaccinationSchedule;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/** 예방접종 일정 응답. */
@Getter
@Builder
public class VaccinationScheduleResponse {

    private final Long id;
    private final Long childId;
    private final String vaccineType;
    private final String vaccineName;
    private final Integer doseNumber;
    private final Integer totalDoses;
    private final LocalDate dueDate;
    private final LocalDate completedDate;
    private final String status;
    private final boolean overdue;

    public static VaccinationScheduleResponse from(VaccinationSchedule schedule) {
        return VaccinationScheduleResponse.builder()
                .id(schedule.getId())
                .childId(schedule.getChild() != null ? schedule.getChild().getId() : null)
                .vaccineType(schedule.getVaccineType().name())
                .vaccineName(schedule.getVaccineType().getDisplayName())
                .doseNumber(schedule.getDoseNumber())
                .totalDoses(schedule.getVaccineType().getTotalDoses())
                .dueDate(schedule.getDueDate())
                .completedDate(schedule.getCompletedDate())
                .status(schedule.getStatus().name())
                .overdue(schedule.isOverdue(LocalDate.now()))
                .build();
    }
}
