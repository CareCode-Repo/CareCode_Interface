package com.carecode.core.scheduler;

import com.carecode.domain.health.entity.VaccinationSchedule;
import com.carecode.domain.health.repository.VaccinationScheduleRepository;
import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.notification.service.NotificationCreationService;
import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** 예방접종 사전 알림. 접종 예정일 D-reminderDaysBefore 구간에 들어온 일정을 찾아 보호자에게 알린다 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VaccinationReminderScheduler {

    private final VaccinationScheduleRepository scheduleRepository;
    private final NotificationCreationService notificationCreationService;

    @Value("${app.scheduler.vaccination.reminder-days-before:7}")
    private int reminderDaysBefore;

    /** 매일 오전 9시. */
    @Scheduled(cron = "${app.scheduler.vaccination.cron:0 0 9 * * *}", zone = "Asia/Seoul")
    @Transactional
    public void sendVaccinationReminders() {
        LocalDate today = LocalDate.now();
        LocalDate until = today.plusDays(reminderDaysBefore);

        List<VaccinationSchedule> targets = scheduleRepository.findPendingReminders(today, until);
        if (targets.isEmpty()) {
            log.debug("예방접종 알림 대상 없음");
            return;
        }

        int sent = 0;
        for (VaccinationSchedule schedule : targets) {
            try {
                Child child = schedule.getChild();
                User parent = child != null ? child.getUser() : null;
                if (parent == null) {
                    continue;
                }

                long daysLeft = today.until(schedule.getDueDate()).getDays();
                String title = "예방접종 일정 안내";
                String message = String.format(
                        "%s의 %s %d차 접종 예정일이 %s 남았습니다. (예정일: %s)",
                        child.getName(),
                        schedule.getVaccineType().getDisplayName(),
                        schedule.getDoseNumber(),
                        daysLeft <= 0 ? "오늘까지" : daysLeft + "일",
                        schedule.getDueDate());

                notificationCreationService.createAndSend(
                        parent, Notification.NotificationType.HEALTH, title, message);

                schedule.markReminderSent();
                sent++;
            } catch (Exception e) {
                // 한 건 실패가 나머지 발송을 막지 않도록 한다.
                log.error("예방접종 알림 발송 실패 - scheduleId={}", schedule.getId(), e);
            }
        }

        scheduleRepository.saveAll(targets);
        log.info("예방접종 알림 발송 완료 - 대상={}, 성공={}", targets.size(), sent);
    }
}
