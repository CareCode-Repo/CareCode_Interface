package com.carecode.domain.user.service;

import com.carecode.core.analytics.EventLogger;
import com.carecode.core.analytics.EventType;
import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.annotation.RequireAuthentication;
import com.carecode.core.exception.UserNotFoundException;
import com.carecode.domain.user.dto.request.PasswordChangeRequestDto;
import com.carecode.domain.user.dto.response.UserDto;
import com.carecode.domain.user.dto.response.UserStatsResponse;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.entity.UserRole;
import com.carecode.domain.user.entity.Gender;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/** 사용자 서비스 클래스 사용자 관련 비즈니스 로직 처리 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    /** 자가 가입으로 만들어질 수 있는 유일한 역할. 그 이상은 관리자만 부여한다. */
    private static final UserRole SELF_SIGNUP_ROLE = UserRole.PARENT;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;
    private final EventLogger eventLogger;

    // 사용자 상세 조회 (String ID) - 삭제되지 않은 사용자만
    @LogExecutionTime
    public UserDto getUserById(String userId) {
            // 먼저 userId로 조회 시도 (삭제되지 않은 사용자만)
            Optional<User> userByUserId = userRepository.findByUserIdAndDeletedAtIsNull(userId);
            if (userByUserId.isPresent()) {
                return convertToDto(userByUserId.get());
            }
            
            // userId로 찾지 못한 경우 Long ID로 시도
            Long id = Long.parseLong(userId);
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
            
            // 삭제된 사용자인지 확인
            if (user.getDeletedAt() != null) {
                throw new UserNotFoundException("삭제된 사용자입니다: " + userId);
            }
            
            return convertToDto(user);
    }

    // 이메일로 사용자 조회 (삭제되지 않은 사용자만)
    @LogExecutionTime
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + email));
        
        return convertToDto(user);
    }

    // 이메일로 사용자 Optional 조회 (삭제되지 않은 사용자만)
    @LogExecutionTime
    public Optional<User> getUserByEmailOptional(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email);
    }

    // 이메일로 User 엔티티 조회 (비밀번호 포함) - 삭제되지 않은 사용자만
    @LogExecutionTime
    public User getUserEntityByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + email));
    }

    /** 예외를 던지지 않는 조회. 로그인처럼 "존재하지 않음"과 "비밀번호 불일치"를 구분해서 응답하면 안 되는 경로에서 사용한다. */
    public Optional<User> findActiveUserEntityByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email);
    }

    // User 엔티티 저장
    @Transactional
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    // 카카오 API를 통해 사용자 정보 조회
    public Map<String, Object> getKakaoUserInfo(String accessToken) {
        log.info("카카오 사용자 정보 조회 시작: accessToken={}", accessToken != null ? accessToken.substring(0, Math.min(20, accessToken.length())) + "..." : "null");
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
            );
            
            Map<String, Object> body = response.getBody();
            Map<String, Object> userInfo = new HashMap<>();
            
            if (body != null) {
                // 카카오 ID는 필수값
                Object kakaoId = body.get("id");
                if (kakaoId == null) {
                    throw new RuntimeException("카카오 사용자 ID를 가져올 수 없습니다.");
                }
                userInfo.put("id", kakaoId);
                
                Object kakaoAccountObj = body.get("kakao_account");
                if (kakaoAccountObj instanceof Map<?, ?> kakaoAccount) {
                    Object profileObj = kakaoAccount.get("profile");
                    if (profileObj instanceof Map<?, ?> profile) {
                        userInfo.put("nickname", profile.get("nickname"));
                        userInfo.put("profileImageUrl", profile.get("profile_image_url"));
                    }
                }
            } else {
                throw new RuntimeException("카카오 API 응답이 비어있습니다.");
            }
            
            log.info("카카오 사용자 정보 조회 성공: id={}", userInfo.get("id"));
            return userInfo;
            
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("카카오 API HTTP 에러: 상태코드={}, 응답본문={}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 401) {
                throw new RuntimeException("카카오 액세스 토큰이 유효하지 않습니다. 토큰을 확인해주세요.", e);
            } else {
                throw new RuntimeException("카카오 API 호출 중 오류가 발생했습니다: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("카카오 사용자 정보 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("카카오 사용자 정보를 가져올 수 없습니다.", e);
        }
    }

    // 사용자 통계 조회
    @LogExecutionTime
    public UserStatsResponse getUserStatistics() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countActiveUsersNotDeleted();
        long verifiedUsers = userRepository.countEmailVerifiedUsersNotDeleted();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfWeek = now.minusWeeks(1);
        LocalDateTime startOfMonth = now.minusMonths(1);

        long newUsersToday = userRepository.countNewUsersSince(startOfToday);
        long newUsersThisWeek = userRepository.countNewUsersSince(startOfWeek);
        long newUsersThisMonth = userRepository.countNewUsersSince(startOfMonth);

        return UserStatsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .verifiedUsers(verifiedUsers)
                .newUsersToday(newUsersToday)
                .newUsersThisWeek(newUsersThisWeek)
                .newUsersThisMonth(newUsersThisMonth)
                .build();
    }

    // 카카오 신규 사용자 가입 완료 (이름 및 역할 설정)
    @Transactional
    public UserDto completeKakaoRegistration(String email, String name, String role) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("이름은 필수입니다.");
        }
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("역할은 필수입니다.");
        }
        
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + email));
        
        // 카카오 사용자인지 확인
        if (!"kakao".equals(user.getProvider())) {
            throw new IllegalArgumentException("카카오 사용자가 아닙니다: " + email);
        }
        
        // 이미 가입 완료된 사용자인지 확인
        if (user.getRegistrationCompleted()) {
            throw new IllegalArgumentException("이미 가입 완료된 사용자입니다: " + email);
        }
        
        // 역할 유효성 검증
        UserRole userRole;
        try {
            userRole = UserRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 역할입니다: " + role + ". 가능한 역할: PARENT, CAREGIVER, ADMIN, GUEST");
        }
        
        // 이름 및 역할 업데이트 및 가입 프로세스 완료 처리
        user.setName(name);
        user.setRole(userRole);
        user.setRegistrationCompleted(true); // 가입 프로세스 완료로 설정
        user.setUpdatedAt(LocalDateTime.now());
        
        User updatedUser = userRepository.save(user);
        log.info("카카오 사용자 가입 프로세스 완료: email={}, name={}, role={}", email, name, role);
        return convertToDto(updatedUser);
    }

    // 사용자 생성
    @Transactional
    public UserDto createUser(UserDto userDto) {
        log.info("사용자 생성: 이메일={}, provider={}", userDto.getEmail(), userDto.getProvider());

        // 이메일 중복 확인
        if (userRepository.findByEmail(userDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다: " + userDto.getEmail());
        }

        // 이메일 회원가입이므로 비밀번호는 항상 필수다.
        if (userDto.getPassword() == null || userDto.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호는 필수입니다.");
        }
        String encodedPassword = passwordEncoder.encode(userDto.getPassword());

        // 클라이언트가 보낸 role 은 신뢰하지 않는다.
        //
        // 예전에는 요청 본문의 role 을 그대로 썼다. 이 엔드포인트(POST /auth/register)는
        // permitAll 이므로, 로그인조차 없이 {"role":"ADMIN"} 으로 가입하면 그 자리에서
        // 관리자가 됐다. 가입은 언제나 일반 사용자로 끝나야 하고, 승격은 관리자만 할 수 있는
        // 별도 경로(PUT /api/admin/users/{id}/role)로만 가능해야 한다.
        if (userDto.getRole() != null && !SELF_SIGNUP_ROLE.name().equals(userDto.getRole())) {
            log.warn("회원가입 요청의 role 을 무시합니다 - 요청값={}, 적용값={}",
                    userDto.getRole(), SELF_SIGNUP_ROLE);
        }

        // provider/providerId 도 마찬가지다. 소셜 가입은 AuthServiceImpl 의 별도 경로가 처리하며
        // 그쪽에서 provider 를 직접 지정한다. 여기서 클라이언트가 provider 를 붙일 수 있게 두면
        // 비밀번호 없이(로그인 불가하지만) 임의 이메일·임의 providerId 로 계정을 미리 만들어 둘 수 있고,
        // 이메일 인증도 건너뛴 것으로 표시됐다.
        User user = User.builder()
                .email(userDto.getEmail())
                .password(encodedPassword)
                .name(userDto.getName())
                .phoneNumber(userDto.getPhoneNumber())
                .birthDate(userDto.getBirthDate())
                .gender(userDto.getGender() != null ? Gender.valueOf(userDto.getGender()) : null)
                .address(userDto.getAddress())
                .profileImageUrl(userDto.getProfileImageUrl())
                .role(SELF_SIGNUP_ROLE)
                .provider(null)
                .providerId(null)
                .isActive(true)
                .emailVerified(false) // 인증 메일을 통과해야 true 가 된다
                .build();

        User savedUser = userRepository.save(user);
        eventLogger.log(EventType.SIGNED_UP, savedUser.getId(), null);
        return convertToDto(savedUser);
    }

    // 비밀번호 변경 (PasswordChangeRequestDto)
    @Transactional
    @RequireAuthentication
    public void changePassword(String userId, PasswordChangeRequestDto request) {
        Long id = Long.parseLong(userId);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호로 변경
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    // 사용자 비활성화 (String ID)
    @Transactional
    @RequireAuthentication
    public void deactivateUser(String userId) {
        log.info("사용자 비활성화: 사용자ID={}", userId);

        try {
            Long id = Long.parseLong(userId);
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
            user.setIsActive(false);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("잘못된 사용자 ID 형식입니다: " + userId);
        }
    }

    // 사용자 활성화 (String ID) — 남의 계정 상태를 바꾸는 동작이라 관리자만 호출할 수 있다.
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void activateUser(String userId) {
        log.info("사용자 활성화: 사용자ID={}", userId);

        try {
            Long id = Long.parseLong(userId);
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
            user.setIsActive(true);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("잘못된 사용자 ID 형식입니다: " + userId);
        }
    }

    /**
     * 사용자 역할 변경.
     *
     * <p>권한 상승의 유일한 경로라 컨트롤러 매핑에만 의존하지 않는다. 호출 경로가 어디로 바뀌든
     * ADMIN 이 아니면 여기서 막힌다. (예전에는 이 메서드가 {@code PUT /users/{id}/role} 로
     * 노출돼 있었고 그 경로의 제약이 "로그인만 하면 됨" 이어서, 아무 회원이나 자신을
     * ADMIN 으로 올릴 수 있었다.)
     *
     * <p>알 수 없는 역할 문자열이 오면 {@code UserRole.valueOf} 가 IllegalArgumentException 을
     * 던지고, 전역 핸들러가 400 으로 변환한다.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void updateUserRole(Long userId, String newRole) {
        log.info("사용자 역할 변경: 사용자ID={}, 새 역할={}", userId, newRole);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        UserRole role;
        try {
            role = UserRole.valueOf(newRole.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 역할입니다: " + newRole);
        }

        user.setRole(role);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    // 프로필 이미지 업데이트 (String ID)
    @Transactional
    @RequireAuthentication
    public void updateProfileImage(String userId, String imageUrl) {
        log.info("프로필 이미지 업데이트: 사용자ID={}", userId);

        try {
            Long id = Long.parseLong(userId);
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
            user.setProfileImageUrl(imageUrl);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("잘못된 사용자 ID 형식입니다: " + userId);
        }
    }

    // 사용자 검색 (삭제되지 않은 사용자만)
    @LogExecutionTime
    @RequireAuthentication
    public List<UserDto> searchUsers(String keyword) {
        List<User> users = userRepository.findByNameContainingOrEmailContainingAndDeletedAtIsNull(keyword, keyword);
        return users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 사용자 검색 (타입별, 삭제되지 않은 사용자만)
    @LogExecutionTime
    @RequireAuthentication
    public List<UserDto> searchUsers(String keyword, String type) {
        List<User> users;
        switch (type.toLowerCase()) {
            case "name":
                users = userRepository.findByNameContainingAndDeletedAtIsNull(keyword);
                break;
            case "email":
                users = userRepository.findByEmailContainingAndDeletedAtIsNull(keyword);
                break;
            default:
                throw new IllegalArgumentException("지원하지 않는 검색 타입입니다: " + type);
        }
        
        return users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Entity를 DTO로 변환
    public UserDto convertToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .phoneNumber(user.getPhoneNumber())
                .birthDate(user.getBirthDate())
                .gender(user.getGender() != null ? String.valueOf(user.getGender()) : null)
                .address(user.getAddress())
                .profileImageUrl(user.getProfileImageUrl())
                .role(String.valueOf(user.getRole()))
                .provider(user.getProvider()) // OAuth 제공자 정보 추가
                .providerId(user.getProviderId()) // OAuth 제공자 ID 추가
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .registrationCompleted(user.getRegistrationCompleted()) // 가입 프로세스 완료 여부 추가
                .deletedAt(user.getDeletedAt()) // 소프트 삭제 시간 추가
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    // 사용자 위치 업데이트
    @Transactional
    public UserDto updateUserLocation(String userId, Double latitude, Double longitude) {
        Long id = Long.parseLong(userId);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        user.setLatitude(latitude);
        user.setLongitude(longitude);
        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);
        return convertToDto(updatedUser);
    }

    // 활성화된 사용자 목록 조회
    @LogExecutionTime
    public List<UserDto> getActiveUsers() {
        List<User> users = userRepository.findByIsActiveTrue();
        return users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 사용자 유형별 조회
    @LogExecutionTime
    public List<UserDto> getUsersByType(String userType) {
        List<User> users = userRepository.findByRole(userType);
        return users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 지역별 사용자 조회
    @LogExecutionTime
    public List<UserDto> getUsersByRegion(String region) {
        List<User> users = userRepository.findByAddressContaining(region);
        return users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 인증된 사용자 목록 조회
    @LogExecutionTime
    public List<UserDto> getVerifiedUsers() {
        List<User> users = userRepository.findByEmailVerifiedTrue();
        return users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 최근 로그인한 사용자 목록 조회
    @LogExecutionTime
    public List<UserDto> getRecentlyActiveUsers() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        List<User> users = userRepository.findByUpdatedAtAfter(oneWeekAgo);
        return users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 사용자 소프트 삭제 (상태값만 변경)
    @Transactional
    public void deleteUser(String userId) {
        User user;
        
        // 먼저 userId로 조회 시도 (삭제되지 않은 사용자만)
        Optional<User> userByUserId = userRepository.findByUserIdAndDeletedAtIsNull(userId);
        if (userByUserId.isPresent()) {
            user = userByUserId.get();
        } else {
            // userId로 찾지 못한 경우 Long ID로 시도
            try {
                Long id = Long.parseLong(userId);
                user = userRepository.findById(id)
                        .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
                
                // 삭제된 사용자인지 확인
                if (user.getDeletedAt() != null) {
                    throw new UserNotFoundException("삭제된 사용자입니다: " + userId);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("잘못된 사용자 ID 형식입니다: " + userId);
            }
        }
        
        // 이미 삭제된 사용자인지 확인
        if (user.getDeletedAt() != null) {
            throw new IllegalArgumentException("이미 삭제된 사용자입니다.");
        }
        
        // 소프트 삭제 처리
        user.setDeletedAt(LocalDateTime.now());
        user.setIsActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * 탈퇴 계정 복구. 관리자 전용이다.
     *
     * <p>탈퇴한 본인은 로그인 자체가 되지 않으므로(비활성 계정은 인증에서 걸린다)
     * "본인이 스스로 복구한다"는 흐름은 성립하지 않는다. 그런데도 이 기능이
     * 로그인만 하면 되는 경로에 열려 있어, 아무 회원이나 남이 탈퇴시킨 계정을
     * 되살릴 수 있었다.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void reactivateUser(String userId) {
        User user;
        
        // 먼저 userId로 조회 시도 (삭제된 사용자 포함)
        Optional<User> userByUserId = userRepository.findByUserId(userId);
        if (userByUserId.isPresent()) {
            user = userByUserId.get();
        } else {
            // userId로 찾지 못한 경우 Long ID로 시도
            try {
                Long id = Long.parseLong(userId);
                user = userRepository.findById(id)
                        .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("잘못된 사용자 ID 형식입니다: " + userId);
            }
        }
        
        // 이미 활성화된 계정인지 확인
        if (user.getIsActive() && user.getDeletedAt() == null) {
            throw new IllegalArgumentException("이미 활성화된 계정입니다.");
        }
        
        // 계정 재활성화 (소프트 삭제 복구 포함)
        user.setIsActive(true);
        user.setDeletedAt(null); // 삭제 시간 초기화
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("사용자 계정 복구 완료: 사용자ID={}, 복구시간={}", userId, user.getUpdatedAt());
    }

    // 역할별 사용자 조회
    @LogExecutionTime
    public List<UserDto> getUsersByRole(String role) {
        log.info("역할별 사용자 조회: 역할={}", role);
        
        List<User> users = userRepository.findByRoleAndDeletedAtIsNull(role);
        return users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 지역별 사용자 조회
    @LogExecutionTime
    public List<UserDto> getUsersByLocation(String region) {
        log.info("지역별 사용자 조회: 지역={}", region);
        
        List<User> users = userRepository.findByAddressContainingAndDeletedAtIsNull(region);
        return users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 이메일 인증된 사용자 조회
    @LogExecutionTime
    public List<UserDto> getEmailVerifiedUsers() {
        log.info("이메일 인증된 사용자 조회");
        
        List<User> users = userRepository.findEmailVerifiedUsersNotDeleted();
        return users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 최근 업데이트된 사용자 조회 -> 나중에 사용 @LogExecutionTime public List<UserDto> getRecentlyUpdatedUsers(int
} 