package com.carecode.domain.health.service;

import com.carecode.core.analytics.EventLogger;
import com.carecode.core.analytics.EventType;
import com.carecode.core.exception.ChildNotFoundException;
import com.carecode.core.security.CurrentUserFacade;
import com.carecode.domain.health.dto.request.ChildCreateRequest;
import com.carecode.domain.health.dto.response.ChildInfoResponse;
import com.carecode.domain.health.mapper.ChildMapper;
import com.carecode.domain.user.app.ChildDirectory;
import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

/** 아이 정보 관리. 등록 시 표준 예방접종 일정을 함께 생성한다. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChildService {

    private final ChildRepository childRepository;
    private final ChildDirectory childDirectory;
    private final ChildMapper childMapper;
    private final CurrentUserFacade currentUserFacade;
    private final VaccinationScheduleService vaccinationScheduleService;
    private final EventLogger eventLogger;

    @Transactional
    public ChildInfoResponse createChild(ChildCreateRequest request) {
        User parent = currentUserFacade.requireCurrentUser();

        Child child = Child.builder()
                .user(parent)
                .name(request.getName())
                .birthDate(request.getBirthDate())
                .age(calculateAge(request.getBirthDate()))
                .gender(request.getGender())
                .specialNeeds(request.getSpecialNeeds())
                .build();

        Child saved = childRepository.save(child);
        eventLogger.log(EventType.CHILD_REGISTERED, parent.getId(), String.valueOf(saved.getId()));
        log.info("아이 등록 - childId={}, userId={}", saved.getId(), parent.getId());

        // 생년월일 기준 표준 접종 일정 자동 생성
        vaccinationScheduleService.generateScheduleForChild(saved.getId());

        return childMapper.toResponse(saved);
    }

    public List<ChildInfoResponse> getMyChildren() {
        User parent = currentUserFacade.requireCurrentUser();
        return childRepository.findByUserIdOrderByCreatedAtDesc(parent.getId()).stream()
                .map(childMapper::toResponse)
                .toList();
    }

    public ChildInfoResponse getChild(Long childId) {
        return childMapper.toResponse(requireOwnedChild(childId));
    }

    @Transactional
    public ChildInfoResponse updateChild(Long childId, ChildCreateRequest request) {
        Child child = requireOwnedChild(childId);

        child.setName(request.getName());
        child.setBirthDate(request.getBirthDate());
        child.setAge(calculateAge(request.getBirthDate()));
        child.setGender(request.getGender());
        child.setSpecialNeeds(request.getSpecialNeeds());

        return childMapper.toResponse(childRepository.save(child));
    }

    @Transactional
    public void deleteChild(Long childId) {
        childRepository.delete(requireOwnedChild(childId));
    }

    /**
     * 소유권을 확인한 아이 엔티티. 다른 서비스(타임라인 등)가 같은 검증을 다시 구현하지 않도록 공개한다.
     * 검증을 복사하면 한쪽만 고쳐져 남의 아이가 열리는 일이 생긴다.
     */
    public Child requireOwned(Long childId) {
        return requireOwnedChild(childId);
    }

    /**
     * 아이 조회 + 소유권 검증. 판단은 {@link ChildDirectory} 한 곳에서만 한다.
     *
     * <p>여기서 엔티티가 필요한 이유: 자녀 수정·삭제가 이 서비스에 있다. 검증 자체는 입구에
     * 맡기고, 통과한 뒤 같은 트랜잭션의 1차 캐시에서 엔티티를 꺼낸다(추가 쿼리가 나가지 않는다).
     */
    private Child requireOwnedChild(Long childId) {
        User parent = currentUserFacade.requireCurrentUser();
        childDirectory.requireOwnedChild(childId, parent.getId());
        return childRepository.findById(childId)
                .orElseThrow(() -> new ChildNotFoundException("아이를 찾을 수 없습니다: " + childId));
    }

    private Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
