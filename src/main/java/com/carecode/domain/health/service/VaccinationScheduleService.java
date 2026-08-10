package com.carecode.domain.health.service;

import com.carecode.core.exception.ChildNotFoundException;
import com.carecode.domain.health.dto.response.VaccinationScheduleResponse;
import com.carecode.domain.health.entity.VaccinationSchedule;
import com.carecode.domain.health.entity.VaccineType;
import com.carecode.domain.health.repository.VaccinationScheduleRepository;
import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 아이별 예방접종 일정 생성·조회. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VaccinationScheduleService {

    private final VaccinationScheduleRepository scheduleRepository;
    private final ChildRepository childRepository;

    /** 아이의 생년월일을 기준으로 표준 접종 일정을 생성한다. 이미 일정이 있으면 중복 생성하지 않는다. */
    @Transactional
    public int generateScheduleForChild(Long childId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new ChildNotFoundException("아이를 찾을 수 없습니다: " + childId));

        if (child.getBirthDate() == null) {
            log.warn("생년월일이 없어 접종 일정을 생성할 수 없습니다. childId={}", childId);
            return 0;
        }
        if (scheduleRepository.existsByChildId(childId)) {
            log.debug("이미 접종 일정이 있어 생성을 건너뜁니다. childId={}", childId);
            return 0;
        }

        List<VaccinationSchedule> schedules = new ArrayList<>();
        for (VaccineType vaccine : VaccineType.all()) {
            for (int dose = 1; dose <= vaccine.getTotalDoses(); dose++) {
                LocalDate dueDate = child.getBirthDate().plusMonths(vaccine.getMonthsForDose(dose));
                schedules.add(VaccinationSchedule.builder()
                        .child(child)
                        .vaccineType(vaccine)
                        .doseNumber(dose)
                        .dueDate(dueDate)
                        .status(VaccinationSchedule.VaccinationStatus.SCHEDULED)
                        .build());
            }
        }

        scheduleRepository.saveAll(schedules);
        log.info("예방접종 일정 생성 완료 - childId={}, 건수={}", childId, schedules.size());
        return schedules.size();
    }

    public List<VaccinationScheduleResponse> getSchedule(Long childId) {
        return scheduleRepository.findByChildIdOrderByDueDateAsc(childId).stream()
                .map(VaccinationScheduleResponse::from)
                .toList();
    }

    /** 예정일이 지났는데 아직 접종하지 않은 항목. */
    public List<VaccinationScheduleResponse> getOverdue(Long childId) {
        return scheduleRepository.findOverdue(childId, LocalDate.now()).stream()
                .map(VaccinationScheduleResponse::from)
                .toList();
    }

    @Transactional
    public VaccinationScheduleResponse markCompleted(Long scheduleId, LocalDate completedDate) {
        VaccinationSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("접종 일정을 찾을 수 없습니다: " + scheduleId));

        schedule.markCompleted(completedDate != null ? completedDate : LocalDate.now());
        return VaccinationScheduleResponse.from(scheduleRepository.save(schedule));
    }
}
