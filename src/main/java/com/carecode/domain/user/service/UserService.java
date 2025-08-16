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
     * 사용자 상세 조회 (String ID)
     */
    @LogExecutionTime
    public UserDto getUserById(String userId) {
        log.info("사용자 상세 조회: 사용자ID={}", userId);
        
        try {
            // 먼저 userId로 조회 시도
            Optional<User> userByUserId = userRepository.findByUserId(userId);
            if (userByUserId.isPresent()) {
                return convertToDto(userByUserId.get());
            }
            
            // userId로 찾지 못한 경우 Long ID로 시도
            Long id = Long.parseLong(userId);
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
            
            return convertToDto(user);
        } catch (NumberFormatException e) {
            // Long ID로도 변환할 수 없는 경우
            throw new IllegalArgumentException("잘못된 사용자 ID 형식입니다: " + userId);
        }
    }

    /**
     * 이메일로 사용자 조회
     */
    @LogExecutionTime
    public UserDto getUserByEmail(String email) {
        log.info("이메일로 사용자 조회: 이메일={}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + email));
        
        return convertToDto(user);
    }

    /**
     * 이메일로 사용자 조회 (Optional 반환)
     */
    @LogExecutionTime
    public Optional<UserDto> getUserByEmailOptional(String email) {
        log.info("이메일로 사용자 조회: 이메일={}", email);
        
        return userRepository.findByEmail(email)
                .map(this::convertToDto);
    }

    /**
     * 이메일로 User 엔티티 조회 (비밀번호 포함)
     */
    @LogExecutionTime
    public User getUserEntityByEmail(String email) {
        log.info("이메일로 User 엔티티 조회: 이메일={}", email);
        
        return userRepository.findByEmail(email)
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
     * 이름으로 사용자 조회
     */
    public Optional<User> findByName(String name) {
        log.info("이름으로 사용자 조회: name={}", name);
        return userRepository.findByName(name);
    }

    /**
     * 카카오 API를 통해 사용자 정보 조회
     */
    public Map<String, Object> getKakaoUserInfo(String accessToken) {
        log.info("카카오 사용자 정보 조회 시작");
        
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            Map<String, Object> body = response.getBody();
            Map<String, Object> userInfo = new HashMap<>();
            
            if (body != null) {
                userInfo.put("id", body.get("id"));
                
                Map<String, Object> kakaoAccount = (Map<String, Object>) body.get("kakao_account");
                if (kakaoAccount != null && kakaoAccount.containsKey("profile")) {
                    Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                    userInfo.put("nickname", profile.get("nickname"));
                    userInfo.put("profileImageUrl", profile.get("profile_image_url"));
                }
            }
            
            log.info("카카오 사용자 정보 조회 성공: id={}", userInfo.get("id"));
            return userInfo;
            
        } catch (Exception e) {
            log.error("카카오 사용자 정보 조회 실패: {}", e.getMessage());
            throw new RuntimeException("카카오 사용자 정보를 가져올 수 없습니다.", e);
        }
    }

    /**
     * 사용자 생성
     */
    @Transactional
    public UserDto createUser(UserDto userDto) {
        log.info("사용자 생성: 이메일={}", userDto.getEmail());
        
        // 이메일 중복 확인
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다: " + userDto.getEmail());
        }
        
        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(userDto.getPassword());
        
        // role이 USER인 경우 PARENT로 변경
        String role = userDto.getRole();
        if ("USER".equals(role)) {
            role = "PARENT";
        }
        
        User user = User.builder()
                .email(userDto.getEmail())
                .password(encodedPassword)
                .name(userDto.getName())
                .phoneNumber(userDto.getPhoneNumber())
                .birthDate(userDto.getBirthDate())
                .gender(User.Gender.valueOf(userDto.getGender()))
                .address(userDto.getAddress())
                .profileImageUrl(userDto.getProfileImageUrl())
                .role(User.UserRole.valueOf(role))
                .isActive(true)
                .emailVerified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
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
     * 사용자 검색
     */
    @LogExecutionTime
    @RequireAuthentication
    public List<UserDto> searchUsers(String keyword) {
        log.info("사용자 검색: 키워드={}", keyword);
        
        List<User> users = userRepository.findByNameContainingOrEmailContaining(keyword, keyword);
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
                .gender(String.valueOf(user.getGender()))
                .address(user.getAddress())
                .profileImageUrl(user.getProfileImageUrl())
                .role(String.valueOf(user.getRole()))
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
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
    public void updateUserLocation(String userId, Double latitude, Double longitude) {
        log.info("사용자 위치 업데이트: 사용자ID={}, 위도={}, 경도={}", userId, latitude, longitude);
        
        try {
            Long id = Long.parseLong(userId);
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
            
            user.setLatitude(latitude);
            user.setLongitude(longitude);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
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
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + email));
        
        // 비밀번호 확인
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
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
     * 사용자 통계 조회
     */
    @LogExecutionTime
    @RequireAuthentication
    public UserDto.UserStats getUserStatistics() {
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
     * 사용자 삭제
     */
    @Transactional
    public void deleteUser(String userId) {
        log.info("사용자 삭제: 사용자ID={}", userId);
        
        try {
            Long id = Long.parseLong(userId);
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
            
            userRepository.delete(user);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("잘못된 사용자 ID 형식입니다: " + userId);
        }
    }

    public Long getUserIdFromUserDetails(UserDetails userDetails) {
        // 예시: username에 email이 들어있다고 가정
        String email = userDetails.getUsername();
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
    }
} 