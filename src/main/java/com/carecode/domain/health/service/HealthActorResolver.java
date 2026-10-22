package com.carecode.domain.health.service;

import com.carecode.core.exception.BusinessException;
import com.carecode.core.exception.ErrorCode;
import com.carecode.core.exception.UserNotFoundException;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * health API 가 받은 {@code userId} 를 요청자 본인으로 확인하고 사용자를 꺼낸다.
 *
 * <p>health 쪽 조회 API 는 {@code userId} 를 문자열로 받는다. 그 값이 요청자 본인인지 확인하는
 * 코드가 {@code HealthService} 안에 세 조각(검증·조회·본인 확인)으로 흩어져 있었고, 통계·알림·
 * 추천을 따로 떼면 양쪽이 그 세 조각을 각자 들고 가게 된다. 소유권 검증을 복제하면 한쪽이
 * 조용히 뒤처진다는 것은 자녀 쪽에서 이미 겪었다({@code ChildDirectory}).
 *
 * <p>합치면서 쿼리도 하나 줄었다. 전에는 본인 확인에서 한 번, 사용자 조회에서 또 한 번
 * 읽었다.
 *
 * <p><b>다음 자리</b>: 이것은 사실 {@code user} 도메인의 일이다. health 가 아직
 * {@code UserRepository} 를 직접 쓰는 이유가 여기뿐이므로, {@code ChildDirectory} 처럼
 * 사용자 쪽 입구로 옮기면 그 의존이 사라진다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HealthActorResolver {

    private final UserRepository userRepository;

    /**
     * {@code userId} 가 요청자 본인인지 확인하고 사용자를 돌려준다.
     *
     * @throws BusinessException 요청자를 알 수 없거나({@code 401}) 다른 사용자일 때({@code 403})
     */
    public User requireSelf(String userId, Long actorUserId) {
        if (actorUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "인증이 필요합니다.");
        }
        User resolved = resolve(userId);
        if (!resolved.getId().equals(actorUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "해당 사용자 정보에 접근할 권한이 없습니다.");
        }
        return resolved;
    }

    /**
     * {@code userId} 로 사용자를 찾는다.
     *
     * <p>이 값은 가입 시 발급한 문자열 {@code userId} 일 수도, PK 를 문자열로 적은 것일 수도 있다.
     * 두 가지가 섞여 들어오는 것이 애초에 혼란이지만, 기존 호출자가 양쪽을 보내고 있어 둘 다 받는다.
     */
    public User resolve(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "사용자 ID가 필요합니다.");
        }

        Optional<User> byUserId = userRepository.findByUserId(userId);
        if (byUserId.isPresent()) {
            return byUserId.get();
        }

        try {
            return userRepository.findById(Long.parseLong(userId))
                    .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        } catch (NumberFormatException e) {
            throw new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId);
        }
    }
}
