package com.carecode.domain.facility.service;

import com.carecode.core.analytics.EventLogger;
import com.carecode.core.analytics.EventType;
import com.carecode.core.exception.BusinessException;
import com.carecode.core.exception.ErrorCode;
import com.carecode.core.exception.ResourceNotFoundException;
import com.carecode.core.security.CurrentUserFacade;
import com.carecode.domain.facility.dto.request.WaitlistRequest;
import com.carecode.domain.facility.dto.response.WaitlistStatsResponse;
import com.carecode.domain.facility.entity.CareFacility;
import com.carecode.domain.facility.entity.FacilityWaitlist;
import com.carecode.domain.facility.repository.CareFacilityRepository;
import com.carecode.domain.facility.repository.FacilityWaitlistRepository;
import com.carecode.domain.user.app.ChildDirectory;
import com.carecode.domain.user.app.ChildView;
import com.carecode.domain.user.entity.User;
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
    private final ChildDirectory childDirectory;
    private final CurrentUserFacade currentUserFacade;
    private final EventLogger eventLogger;

    @Transactional
    public Long register(Long facilityId, WaitlistRequest request) {
        User user = currentUserFacade.requireCurrentUser();
        CareFacility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("시설을 찾을 수 없습니다: " + facilityId));
        ChildView child = resolveOwnChild(user, request.getChildId());

        // 같은 아이·같은 시설의 중복 등록은 통계를 왜곡하므로 기존 기록을 그대로 돌려준다.
        var existing = waitlistRepository.findByFacilityIdAndChildId(facilityId, child.childId());
        if (existing.isPresent()) {
            return existing.get().getId();
        }

        FacilityWaitlist saved = waitlistRepository.save(FacilityWaitlist.builder()
                .facilityId(facility.getId())
                .user(user)
                .childId(child.childId())
                .waitNumber(request.getWaitNumber())
                .appliedAt(request.getAppliedAt() != null ? request.getAppliedAt() : LocalDate.now())
                .status(FacilityWaitlist.WaitStatus.WAITING)
                .classAge(monthsOld(child))
                .note(request.getNote())
                .build());
        eventLogger.log(EventType.WAITLIST_REGISTERED, user.getId(), String.valueOf(facilityId));
        return saved.getId();
    }

    /** 입소·포기 처리. 이 시점이 찍혀야 대기 기간 데이터가 완성된다. */
    @Transactional
    public void resolve(Long waitlistId, String status, LocalDate resolvedAt, String note) {
        User user = currentUserFacade.requireCurrentUser();
        FacilityWaitlist entry = waitlistRepository.findById(waitlistId)
                .orElseThrow(() -> new ResourceNotFoundException("대기 기록을 찾을 수 없습니다: " + waitlistId));

        if (!entry.getUser().getId().equals(user.getId())) {
            // 403 이어야 한다. CareServiceException 은 기본 매핑이 500 이라, 남의 기록을
            // 수정하려는 요청에 서버 오류가 나가고 운영 알림까지 울렸다.
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인의 대기 기록만 수정할 수 있습니다.");
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
                .orElseThrow(() -> new ResourceNotFoundException("시설을 찾을 수 없습니다: " + facilityId));

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

    /**
     * 남의 아이로 등록하지 못하게 소유권을 확인한다. 지정하지 않으면 최근에 등록한 자녀다.
     *
     * <p>두 경우 모두 사용자가 고칠 수 있는 상태라 400 이어야 한다. 전에는
     * {@code CareServiceException} 이어서 500 이 나가고 운영 알림까지 울렸다.
     */
    private ChildView resolveOwnChild(User user, Long childId) {
        if (childId != null) {
            return childDirectory.requireOwnedChild(childId, user.getId());
        }
        List<ChildView> children = childDirectory.childrenOf(user.getId());
        if (children.isEmpty()) {
            throw new BusinessException("등록된 자녀가 없습니다.");
        }
        return children.get(0);
    }

    private Integer monthsOld(ChildView child) {
        return child.ageMonths(LocalDate.now());
    }
}
