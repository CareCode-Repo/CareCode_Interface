package com.carecode.domain.health.service;

import com.carecode.core.exception.ChildNotFoundException;
import com.carecode.core.security.CurrentUserFacade;
import com.carecode.domain.health.dto.request.ChildCreateRequest;
import com.carecode.domain.health.dto.response.ChildInfoResponse;
import com.carecode.domain.health.mapper.ChildMapper;
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

/**
 * 아이 정보 관리.
 *
 * <p>등록 시 표준 예방접종 일정을 함께 생성한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChildService {

    private final ChildRepository childRepository;
    private final ChildMapper childMapper;
    private final CurrentUserFacade currentUserFacade;
    private final VaccinationScheduleService vaccinationScheduleService;

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
     * 아이 조회 + 소유권 검증.
     * 남의 아이 정보에 접근하지 못하도록 보호자 본인 것만 반환한다.
     */
    private Child requireOwnedChild(Long childId) {
        User parent = currentUserFacade.requireCurrentUser();
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new ChildNotFoundException("아이를 찾을 수 없습니다: " + childId));

        if (child.getUser() == null || !child.getUser().getId().equals(parent.getId())) {
            // 존재 여부 자체를 숨기기 위해 404 로 응답한다.
            throw new ChildNotFoundException("아이를 찾을 수 없습니다: " + childId);
        }
        return child;
    }

    private Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
