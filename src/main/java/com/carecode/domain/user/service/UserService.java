package com.carecode.domain.user.service;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.annotation.RequireAuthentication;
import com.carecode.core.exception.UserNotFoundException;
import com.carecode.domain.user.dto.LoginRequestDto;
import com.carecode.domain.user.dto.PasswordChangeRequestDto;
import com.carecode.domain.user.dto.UserDto;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * 사용자 서비스 클래스
 * 사용자 관련 비즈니스 로직 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 사용자 목록 조회
     */
    @LogExecutionTime
    @RequireAuthentication
    public List<UserDto> getAllUsers() {
        log.info("전체 사용자 목록 조회");
        
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 사용자 상세 조회 (Long ID)
     */
    @LogExecutionTime
    public UserDto getUserById(Long userId) {
        log.info("사용자 상세 조회: 사용자ID={}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        
        return convertToDto(user);
    }

    /**
     * 사용자 상세 조회 (String ID) - 삭제되지 않은 사용자만
     */
    @LogExecutionTime
    public UserDto getUserById(String userId) {
        log.info("사용자 상세 조회: 사용자ID={}", userId);
        
        try {
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
        } catch (NumberFormatException e) {
            // Long ID로도 변환할 수 없는 경우
            throw new IllegalArgumentException("잘못된 사용자 ID 형식입니다: " + userId);
        }
    }

    /**
     * 이메일로 사용자 조회 (삭제되지 않은 사용자만)
     */
    @LogExecutionTime
    public UserDto getUserByEmail(String email) {
        log.info("이메일로 사용자 조회: 이메일={}", email);
        
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + email));
        
        return convertToDto(user);
    }

    /**
     * 이메일로 사용자 조회 (Optional 반환) - 삭제되지 않은 사용자만
     */
    @LogExecutionTime
    public Optional<UserDto> getUserByEmailOptional(String email) {
        log.info("이메일로 사용자 조회: 이메일={}", email);
        
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .map(this::convertToDto);
    }

    /**
     * 이메일로 User 엔티티 조회 (비밀번호 포함) - 삭제되지 않은 사용자만
     */
    @LogExecutionTime
    public User getUserEntityByEmail(String email) {
        log.info("이메일로 User 엔티티 조회: 이메일={}", email);
        
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + email));
    }

    /**
     * User 엔티티 저장
     */
    @Transactional
    public User saveUser(User user) {
        log.info("User 엔티티 저장: 이메일={}", user.getEmail());
        return userRepository.save(user);
    }

    /**
     * 이름으로 사용자 조회 (삭제되지 않은 사용자만)
     */
    public Optional<User> findByName(String name) {
        log.info("이름으로 사용자 조회: name={}", name);
        return userRepository.findByNameAndDeletedAtIsNull(name);
    }

    /**
     * 이름으로 사용자 조회 (DTO 반환)
     */
    @LogExecutionTime
    public UserDto getUserByName(String name) {
        log.info("이름으로 사용자 조회: name={}", name);
        
        User user = userRepository.findByNameAndDeletedAtIsNull(name)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + name));
        
        return convertToDto(user);
    }

    /**
     * 이름으로 사용자 조회 (Entity 반환)
     */
    @LogExecutionTime
    public User getUserEntityByName(String name) {
        log.info("이름으로 사용자 엔티티 조회: name={}", name);
        
        return userRepository.findByNameAndDeletedAtIsNull(name)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + name));
    }

    /**
     * 카카오 API를 통해 사용자 정보 조회
     */
    public Map<String, Object> getKakaoUserInfo(String accessToken) {
        log.info("카카오 사용자 정보 조회 시작: accessToken={}", accessToken != null ? accessToken.substring(0, Math.min(20, accessToken.length())) + "..." : "null");
        
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            log.info("카카오 API 호출: URL=https://kapi.kakao.com/v2/user/me");
            log.debug("요청 헤더: {}", headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            log.info("카카오 API 응답 상태: {}", response.getStatusCode());
            
            Map<String, Object> body = response.getBody();
            Map<String, Object> userInfo = new HashMap<>();
            
            if (body != null) {
                // 카카오 ID는 필수값
                Object kakaoId = body.get("id");
                if (kakaoId == null) {
                    throw new RuntimeException("카카오 사용자 ID를 가져올 수 없습니다.");
                }
                userInfo.put("id", kakaoId);
                
                Map<String, Object> kakaoAccount = (Map<String, Object>) body.get("kakao_account");
                if (kakaoAccount != null && kakaoAccount.containsKey("profile")) {
                    Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                    if (profile != null) {
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

    /**
     * 카카오 신규 사용자 가입 완료 (이름 및 역할 설정)
     */
    @Transactional
    public UserDto completeKakaoRegistration(String email, String name, String role) {
        log.info("카카오 신규 사용자 가입 완료: email={}, name={}, role={}", email, name, role);
        
        // 입력값 검증
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("이름은 필수입니다.");
        }
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("역할은 필수입니다.");
        }
        
        // 사용자 조회 (삭제되지 않은 사용자만)
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
        User.UserRole userRole;
        try {
            userRole = User.UserRole.valueOf(role);
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

    /**
     * 사용자 생성
     */
    @Transactional
    public UserDto createUser(UserDto userDto) {
        log.info("사용자 생성: 이메일={}, provider={}", userDto.getEmail(), userDto.getProvider());
        
        // 이메일 중복 확인
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다: " + userDto.getEmail());
        }
        
        // OAuth 사용자와 일반 사용자 구분
        String encodedPassword = null;
        if (userDto.getProvider() == null || userDto.getProvider().isEmpty()) {
            // 일반 회원가입 사용자 - 비밀번호 필수
            if (userDto.getPassword() == null || userDto.getPassword().trim().isEmpty()) {
                throw new IllegalArgumentException("일반 회원가입 사용자는 비밀번호가 필수입니다.");
            }
            encodedPassword = passwordEncoder.encode(userDto.getPassword());
        }
        // OAuth 사용자는 비밀번호가 null (카카오에서 인증)
        
        // role이 USER인 경우 PARENT로 변경
        String role = userDto.getRole();
        if ("USER".equals(role)) {
            role = "PARENT";
        }
        
        User user = User.builder()
                .email(userDto.getEmail())
                .password(encodedPassword) // OAuth 사용자는 null
                .name(userDto.getName())
                .phoneNumber(userDto.getPhoneNumber())
                .birthDate(userDto.getBirthDate())
                .gender(userDto.getGender() != null ? User.Gender.valueOf(userDto.getGender()) : null)
                .address(userDto.getAddress())
                .profileImageUrl(userDto.getProfileImageUrl())
                .role(User.UserRole.valueOf(role))
                .provider(userDto.getProvider()) // OAuth 제공자 정보
                .providerId(userDto.getProviderId()) // OAuth 제공자 ID
                .isActive(true)
                .emailVerified(userDto.getProvider() != null) // OAuth 사용자는 이메일 인증 완료로 간주
                .build();
        
        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }

    /**
     * 사용자 정보 수정 (Long ID)
     */
    @Transactional
    @RequireAuthentication
    public UserDto updateUser(Long userId, UserDto userDto) {
        log.info("사용자 정보 수정: 사용자ID={}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        
        // 수정 가능한 필드들만 업데이트
        user.setName(userDto.getName());
        user.setPhoneNumber(userDto.getPhoneNumber());
        user.setAddress(userDto.getAddress());
        user.setProfileImageUrl(userDto.getProfileImageUrl());
        user.setUpdatedAt(LocalDateTime.now());
        
        User updatedUser = userRepository.save(user);
        return convertToDto(updatedUser);
    }

    /**
     * 사용자 정보 수정 (String ID)
     */
    @Transactional
    @RequireAuthentication
    public UserDto updateUser(String userId, UserDto userDto) {
        log.info("사용자 정보 수정: 사용자ID={}", userId);
        
        try {
            Long id = Long.parseLong(userId);
            return updateUser(id, userDto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("잘못된 사용자 ID 형식입니다: " + userId);
        }
    }

    /**
     * 비밀번호 변경 (Long ID)
     */
    @Transactional
    @RequireAuthentication
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        log.info("비밀번호 변경: 사용자ID={}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        
        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
        
        // 새 비밀번호 암호화 및 저장
        String encodedNewPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedNewPassword);
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
    }

    /**
     * 비밀번호 변경 (String ID)
     */
    @Transactional
    @RequireAuthentication
    public void changePassword(String userId, String currentPassword, String newPassword) {
        log.info("비밀번호 변경: 사용자ID={}", userId);
        
        try {
            Long id = Long.parseLong(userId);
            changePassword(id, currentPassword, newPassword);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("잘못된 사용자 ID 형식입니다: " + userId);
        }
    }

    /**
     * 비밀번호 변경 (PasswordChangeRequestDto)
     */
    @Transactional
    @RequireAuthentication
    public void changePassword(String userId, PasswordChangeRequestDto request) {
        log.info("비밀번호 변경: 사용자ID={}", userId);
        
        try {
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
            
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("잘못된 사용자 ID 형식입니다: " + userId);
        }
    }

    /**
     * 사용자 비활성화 (Long ID)
     */
    @Transactional
    @RequireAuthentication
    public void deactivateUser(Long userId) {
        log.info("사용자 비활성화: 사용자ID={}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        
        user.setIsActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
    }

    /**
     * 사용자 비활성화 (String ID)
     */
    @Transactional
    @RequireAuthentication
    public void deactivateUser(String userId) {
        log.info("사용자 비활성화: 사용자ID={}", userId);
        
        try {
            Long id = Long.parseLong(userId);
            deactivateUser(id);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("잘못된 사용자 ID 형식입니다: " + userId);
        }
    }

    /**
     * 사용자 활성화 (Long ID)
     */
    @Transactional
    @RequireAuthentication
    public void activateUser(Long userId) {
        log.info("사용자 활성화: 사용자ID={}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        
        user.setIsActive(true);
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
    }

    /**
     * 사용자 활성화 (String ID)
     */
    @Transactional
    @RequireAuthentication
    public void activateUser(String userId) {
        log.info("사용자 활성화: 사용자ID={}", userId);
        
        try {
            Long id = Long.parseLong(userId);
            activateUser(id);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("잘못된 사용자 ID 형식입니다: " + userId);
        }
    }

    /**
     * 이메일 인증 완료
     */
    @Transactional
    public void verifyEmail(String email) {
        log.info("이메일 인증 완료: 이메일={}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + email));
        
        user.setEmailVerified(true);
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
    }

    /**
     * 사용자 역할 변경
     */
    @Transactional
    @RequireAuthentication
    public void updateUserRole(Long userId, String newRole) {
        log.info("사용자 역할 변경: 사용자ID={}, 새 역할={}", userId, newRole);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        
        user.setRole(User.UserRole.valueOf(newRole));
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
    }

    /**
     * 프로필 이미지 업데이트 (Long ID)
     */
    @Transactional
    @RequireAuthentication
    public void updateProfileImage(Long userId, String imageUrl) {
        log.info("프로필 이미지 업데이트: 사용자ID={}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        
        user.setProfileImageUrl(imageUrl);
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
    }

    /**
     * 프로필 이미지 업데이트 (String ID)
     */
    @Transactional
    @RequireAuthentication
    public void updateProfileImage(String userId, String imageUrl) {
        log.info("프로필 이미지 업데이트: 사용자ID={}", userId);
        
        try {
            Long id = Long.parseLong(userId);
            updateProfileImage(id, imageUrl);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("잘못된 사용자 ID 형식입니다: " + userId);
        }
    }

    /**
     * 사용자 통계 조회
     */
    @LogExecutionTime
    @RequireAuthentication
    public UserDto.UserStats getUserStats() {
        log.info("사용자 통계 조회");
        
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActiveTrue();
        long verifiedUsers = userRepository.countByEmailVerifiedTrue();
        
        return UserDto.UserStats.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .verifiedUsers(verifiedUsers)
                .build();
    }

    /**
     * 사용자 검색 (삭제되지 않은 사용자만)
     */
    @LogExecutionTime
    @RequireAuthentication
    public List<UserDto> searchUsers(String keyword) {
        log.info("사용자 검색: 키워드={}", keyword);
        
        List<User> users = userRepository.findByNameContainingOrEmailContainingAndDeletedAtIsNull(keyword, keyword);
        return users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 사용자 검색 (타입별, 삭제되지 않은 사용자만)
     */
    @LogExecutionTime
    @RequireAuthentication
    public List<UserDto> searchUsers(String keyword, String type) {
        log.info("사용자 검색: 키워드={}, 타입={}", keyword, type);
        
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

    /**
     * Entity를 DTO로 변환
     */
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

    /**
     * 사용자 인증
     */
    @Transactional
    public void verifyUser(String userId) {
        log.info("사용자 인증: 사용자ID={}", userId);
        
        try {
            Long id = Long.parseLong(userId);
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
            
            user.setEmailVerified(true);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("잘못된 사용자 ID 형식입니다: " + userId);
        }
    }

    /**
     * 사용자 위치 업데이트
     */
    @Transactional
    public UserDto updateUserLocation(String userId, Double latitude, Double longitude) {
        log.info("사용자 위치 업데이트: 사용자ID={}, 위도={}, 경도={}", userId, latitude, longitude);
        
        try {
            Long id = Long.parseLong(userId);
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
            
            user.setLatitude(latitude);
            user.setLongitude(longitude);
            user.setUpdatedAt(LocalDateTime.now());
            User updatedUser = userRepository.save(user);
            return convertToDto(updatedUser);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("잘못된 사용자 ID 형식입니다: " + userId);
        }
    }

    /**
     * 사용자 ID로 사용자 조회 (String ID)
     */
    @LogExecutionTime
    public UserDto getUserByUserId(String userId) {
        log.info("사용자 ID로 사용자 조회: 사용자ID={}", userId);
        
        try {
            Long id = Long.parseLong(userId);
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
            
            return convertToDto(user);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("잘못된 사용자 ID 형식입니다: " + userId);
        }
    }

    /**
     * 활성화된 사용자 목록 조회
     */
    @LogExecutionTime
    public List<UserDto> getActiveUsers() {
        log.info("활성화된 사용자 목록 조회");
        
        List<User> users = userRepository.findByIsActiveTrue();
        return users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 사용자 유형별 조회
     */
    @LogExecutionTime
    public List<UserDto> getUsersByType(String userType) {
        log.info("사용자 유형별 조회: 유형={}", userType);
        
        List<User> users = userRepository.findByRole(userType);
        return users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 지역별 사용자 조회
     */
    @LogExecutionTime
    public List<UserDto> getUsersByRegion(String region) {
        log.info("지역별 사용자 조회: 지역={}", region);
        
        List<User> users = userRepository.findByAddressContaining(region);
        return users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 인증된 사용자 목록 조회
     */
    @LogExecutionTime
    public List<UserDto> getVerifiedUsers() {
        log.info("인증된 사용자 목록 조회");
        
        List<User> users = userRepository.findByEmailVerifiedTrue();
        return users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 최근 로그인한 사용자 목록 조회
     */
    @LogExecutionTime
    public List<UserDto> getRecentlyActiveUsers() {
        log.info("최근 로그인한 사용자 목록 조회");
        
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        List<User> users = userRepository.findByUpdatedAtAfter(oneWeekAgo);
        return users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 사용자 로그인 (String 파라미터)
     */
    @LogExecutionTime
    public UserDto login(String email, String password) {
        log.info("사용자 로그인: 이메일={}", email);
        
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + email));
        
        // 비밀번호 확인
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        
        // 삭제된 사용자인지 확인
        if (user.getDeletedAt() != null) {
            throw new IllegalArgumentException("삭제된 사용자입니다.");
        }
        
        // 활성화 상태 확인
        if (!user.getIsActive()) {
            throw new IllegalArgumentException("비활성화된 사용자입니다.");
        }
        
        // 마지막 로그인 시간 업데이트
        user.setLastLoginAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        return convertToDto(user);
    }

    /**
     * 사용자 로그인 (LoginRequestDto)
     */
    @LogExecutionTime
    public UserDto login(LoginRequestDto request) {
        log.info("사용자 로그인: 이메일={}", request.getEmail());
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + request.getEmail()));
        
        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        
        // 삭제된 사용자인지 확인
        if (user.getDeletedAt() != null) {
            throw new IllegalArgumentException("삭제된 사용자입니다.");
        }
        
        // 활성화 상태 확인
        if (!user.getIsActive()) {
            throw new IllegalArgumentException("비활성화된 사용자입니다.");
        }
        
        // 마지막 로그인 시간 업데이트
        user.setLastLoginAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        return convertToDto(user);
    }

    /**
     * 사용자 통계 조회 (삭제되지 않은 사용자만)
     */
    @LogExecutionTime
    @RequireAuthentication
    public UserDto.UserStats getUserStatistics() {
        log.info("사용자 통계 조회");
        
        long totalUsers = userRepository.countActiveUsersNotDeleted();
        long activeUsers = userRepository.countActiveUsersNotDeleted();
        long verifiedUsers = userRepository.countEmailVerifiedUsersNotDeleted();
        
        return UserDto.UserStats.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .verifiedUsers(verifiedUsers)
                .build();
    }

    /**
     * 사용자 소프트 삭제 (상태값만 변경)
     */
    @Transactional
    public void deleteUser(String userId) {
        log.info("사용자 소프트 삭제: 사용자ID={}", userId);
        
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
        
        log.info("사용자 소프트 삭제 완료: 사용자ID={}, 삭제시간={}", userId, user.getDeletedAt());
    }

    /**
     * 사용자 계정 복구 (재활성화)
     */
    @Transactional
    public void reactivateUser(String userId) {
        log.info("사용자 계정 복구: 사용자ID={}", userId);
        
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

    public Long getUserIdFromUserDetails(UserDetails userDetails) {
        // 예시: username에 email이 들어있다고 가정
        String email = userDetails.getUsername();
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .map(User::getId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
    }
} 