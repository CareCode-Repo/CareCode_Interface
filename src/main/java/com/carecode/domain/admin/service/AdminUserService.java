package com.carecode.domain.admin.service;

import com.carecode.core.exception.UserNotFoundException;
import com.carecode.domain.admin.dto.AdminUserResponse;
import com.carecode.domain.admin.dto.AdminUserUpdateRequest;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 어드민 사용자 관리.
 *
 * <p>조회·수정·탈퇴 처리를 모았다. 역할 변경·활성화·복구는 사용자 도메인
 * ({@code UserService})이 이미 갖고 있어 그대로 쓴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;

    public Page<AdminUserResponse> list(Pageable pageable) {
        return userRepository.findAll(pageable).map(AdminUserResponse::from);
    }

    public AdminUserResponse detail(Long id) {
        return AdminUserResponse.from(require(id));
    }

    /** 이름·연락처·역할·활성 상태만 바꾼다. 보내지 않은 항목은 유지한다. */
    @Transactional
    public AdminUserResponse update(Long id, AdminUserUpdateRequest request) {
        User user = require(id);

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }

        return AdminUserResponse.from(userRepository.save(user));
    }

    /**
     * 탈퇴 처리. 본인 탈퇴와 <b>같은 구현</b>을 쓴다({@link User#withdraw()}).
     *
     * <p>전에는 여기서 {@code deletedAt}·{@code isActive} 만 바꿔, 이름과 이메일이 그대로
     * 남았다. 이용자가 고객센터로 탈퇴를 요청하면 운영자가 이 경로를 누르므로, 그 경우
     * 개인정보가 파기되지 않은 상태였다.
     */
    @Transactional
    public void withdraw(Long id) {
        User user = require(id);
        user.withdraw();
        userRepository.save(user);
        log.info("관리자 탈퇴 처리 완료 - userId={}", id);
    }

    private User require(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + id));
    }
}
