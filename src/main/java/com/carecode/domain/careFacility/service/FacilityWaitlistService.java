package com.carecode.domain.careFacility.service;

import com.carecode.core.exception.CareServiceException;
import com.carecode.core.security.CurrentUserFacade;
import com.carecode.domain.careFacility.dto.request.WaitlistRequest;
import com.carecode.domain.careFacility.dto.response.WaitlistStatsResponse;
import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.entity.FacilityWaitlist;
import com.carecode.domain.careFacility.repository.CareFacilityRepository;
import com.carecode.domain.careFacility.repository.FacilityWaitlistRepository;
import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 대기 신청과 결과를 기록한다.
 * 정원 관측은 "자리가 났는가" 만 알려줄 뿐, 대기 순번이 언제 도는지는 겪은 사람만 안다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FacilityWaitlistService {

    /** 이보다 표본이 적으면 평균이 우연에 좌우된다. */
    private static final int MIN_SAMPLES = 3;

    private final FacilityWaitlistRepository waitlistRepository;
    private final CareFacilityRepository facilityRepository;
    private final ChildRepository childRepository;
    private final CurrentUserFacade currentUserFacade;

    @Transactional
    public Long register(Long facilityId, WaitlistRequest request) {
        User user = currentUserFacade.requireCurrentUser();
        CareFacility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new CareServiceException("시설을 찾을 수 없습니다: " + facilityId));
        Child child = resolveOwnChild(user, request.getChildId());

        // 같은 아이·같은 시설의 중복 등록은 통계를 왜곡하므로 기존 기록을 그대로 돌려준다.
        var existing = waitlistRepository.findByFacilityIdAndChildId(facilityId, child.getId());
        if (existing.isPresent()) {
            return existing.get().getId();
        }

        FacilityWaitlist saved = waitlistRepository.save(FacilityWaitlist.builder()
                .facilityId(facility.getId())
                .user(user)
                .child(child)
                .waitNumber(request.getWaitNumber())
                .appliedAt(request.getAppliedAt() != null ? request.getAppliedAt() : LocalDate.now())
                .status(FacilityWaitlist.WaitStatus.WAITING)
                .classAge(monthsOld(child))
                .note(request.getNote())
                .build());
        return saved.getId();
    }

    /** 입소·포기 처리. 이 시점이 찍혀야 대기 기간 데이터가 완성된다. */
    @Transactional
    public void resolve(Long waitlistId, String status, LocalDate resolvedAt, String note) {
        User user = currentUserFacade.requireCurrentUser();
        FacilityWaitlist entry = waitlistRepository.findById(waitlistId)
                .orElseThrow(() -> new CareServiceException("대기 기록을 찾을 수 없습니다: " + waitlistId));

        if (!entry.getUser().getId().equals(user.getId())) {
            throw new CareServiceException("본인의 대기 기록만 수정할 수 있습니다.");
        }
        entry.resolve(FacilityWaitlist.WaitStatus.valueOf(status), resolvedAt, note);
    }

    @Transactional(readOnly = true)
    public List<FacilityWaitlist> getMyWaitlists() {
        return waitlistRepository.findByUserIdOrderByAppliedAtDesc(
                currentUserFacade.requireCurrentUser().getId());
    }

    @Transactional(readOnly = true)
    public WaitlistStatsResponse getStats(Long facilityId) {
        CareFacility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new CareServiceException("시설을 찾을 수 없습니다: " + facilityId));

        List<FacilityWaitlist> admitted = waitlistRepository.findAdmitted(facilityId);
        long waiting = waitlistRepository.countWaiting(facilityId);

        WaitlistStatsResponse.WaitlistStatsResponseBuilder base = WaitlistStatsResponse.builder()
                .facilityId(facilityId)
                .facilityName(facility.getName())
                .admittedSamples(admitted.size())
                .currentlyWaiting(waiting);

        if (admitted.size() < MIN_SAMPLES) {
            return base.available(false)
                    .unavailableReason(String.format("입소 기록이 %d건으로 부족합니다. (최소 %d건 필요)",
                            admitted.size(), MIN_SAMPLES))
                    .build();
        }

        List<Long> days = admitted.stream()
                .map(w -> ChronoUnit.DAYS.between(w.getAppliedAt(), w.getResolvedAt()))
                .sorted()
                .toList();

        int average = (int) Math.round(days.stream().mapToLong(Long::longValue).average().orElse(0));
        int median = (int) (long) days.get(days.size() / 2);
        int max = (int) (long) days.get(days.size() - 1);

        return base.available(true)
                .averageWaitDays(average)
                .medianWaitDays(median)
                .maxWaitDays(max)
                .reasons(buildReasons(days.size(), median, waiting))
                .build();
    }

    private List<String> buildReasons(int samples, int median, long waiting) {
        List<String> reasons = new ArrayList<>();
        reasons.add(String.format("입소한 %d명의 실제 기록 기준입니다.", samples));
        reasons.add(String.format("절반이 %d개월 안에 입소했습니다.", Math.max(1, median / 30)));
        if (waiting > 0) {
            reasons.add(String.format("현재 %d명이 대기 중으로 등록해 두었습니다.", waiting));
        }
        return reasons;
    }

    /** 남의 아이로 등록하지 못하게 소유권을 확인한다. */
    private Child resolveOwnChild(User user, Long childId) {
        List<Child> children = childRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (children.isEmpty()) {
            throw new CareServiceException("등록된 자녀가 없습니다.");
        }
        if (childId == null) {
            return children.get(0);
        }
        return children.stream()
                .filter(c -> c.getId().equals(childId))
                .findFirst()
                .orElseThrow(() -> new CareServiceException("본인의 자녀만 등록할 수 있습니다."));
    }

    private Integer monthsOld(Child child) {
        return child.getBirthDate() == null ? null
                : (int) ChronoUnit.MONTHS.between(child.getBirthDate(), LocalDate.now());
    }
}
