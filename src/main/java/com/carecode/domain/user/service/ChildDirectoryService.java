package com.carecode.domain.user.service;

import com.carecode.core.exception.ChildNotFoundException;
import com.carecode.domain.user.app.ChildDirectory;
import com.carecode.domain.user.app.ChildView;
import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * {@link ChildDirectory} 구현. 자녀 조회가 도메인 밖으로 나가는 유일한 길이다.
 *
 * <p>소유권 검증은 여기 한 곳에만 있다. 이전에는 같은 검증이 {@code ChildService},
 * {@code GrowthChartService}, {@code HealthService} 세 군데에 각자 쓰여 있었다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChildDirectoryService implements ChildDirectory {

    private final ChildRepository childRepository;

    @Override
    public List<ChildView> childrenOf(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return childRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(ChildView::from)
                .toList();
    }

    @Override
    public ChildView requireOwnedChild(Long childId, Long parentUserId) {
        return ChildView.from(requireOwnedEntity(childId, parentUserId));
    }

    /**
     * 같은 도메인 안에서 엔티티가 필요할 때 쓴다(자녀 수정·삭제).
     *
     * <p>{@code public} 이 아닌 이유: 엔티티를 도메인 밖으로 내보내면 호출하는 쪽이
     * {@code child.getUser()} 로 사용자 정보까지 타고 들어갈 수 있다.
     */
    Child requireOwnedEntity(Long childId, Long parentUserId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new ChildNotFoundException("아이를 찾을 수 없습니다: " + childId));

        // 남의 자녀도 "없다" 로 끝낸다. 403 과 404 를 나누면 ID 를 훑어 존재 여부를 알아낼 수 있다.
        if (parentUserId == null || child.getUser() == null
                || !parentUserId.equals(child.getUser().getId())) {
            throw new ChildNotFoundException("아이를 찾을 수 없습니다: " + childId);
        }
        return child;
    }
}
