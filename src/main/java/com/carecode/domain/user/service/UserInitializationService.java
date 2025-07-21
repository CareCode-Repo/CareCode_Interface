package com.carecode.domain.user.service;

import com.carecode.domain.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 사용자 초기화 서비스
 * 애플리케이션 시작 시 테스트용 사용자를 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserInitializationService implements CommandLineRunner {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        createTestUsers();
    }

    /**
     * 테스트용 사용자 생성
     */
    private void createTestUsers() {
        try {
            // 테스트 사용자 1
            UserDto testUser1 = UserDto.builder()
                    .email("test1@carecode.com")
                    .password("password123")
                    .name("테스트 사용자 1")
                    .phoneNumber("010-1234-5678")
                    .birthDate(LocalDate.of(1990, 1, 1))
                    .gender("FEMALE")
                    .address("서울시 강남구")
                    .latitude(37.5665)
                    .longitude(126.9780)
                    .role("PARENT")
                    .isActive(true)
                    .emailVerified(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            // 테스트 사용자 2
            UserDto testUser2 = UserDto.builder()
                    .email("test2@carecode.com")
                    .password("password123")
                    .name("테스트 사용자 2")
                    .phoneNumber("010-9876-5432")
                    .birthDate(LocalDate.of(1985, 5, 15))
                    .gender("MALE")
                    .address("서울시 서초구")
                    .latitude(37.4837)
                    .longitude(127.0324)
                    .role("PARENT")
                    .isActive(true)
                    .emailVerified(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            // 관리자 사용자
            UserDto adminUser = UserDto.builder()
                    .email("admin@carecode.com")
                    .password("admin123")
                    .name("관리자")
                    .phoneNumber("010-0000-0000")
                    .birthDate(LocalDate.of(1980, 10, 10))
                    .gender("MALE")
                    .address("서울시 중구")
                    .latitude(37.5665)
                    .longitude(126.9780)
                    .role("ADMIN")
                    .isActive(true)
                    .emailVerified(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            // 사용자 생성 시도
            createUserIfNotExists(testUser1, "테스트 사용자 1");
            createUserIfNotExists(testUser2, "테스트 사용자 2");
            createUserIfNotExists(adminUser, "관리자 사용자");

        } catch (Exception e) {
            log.error("테스트 사용자 생성 중 오류 발생: {}", e.getMessage());
        }
    }

    /**
     * 사용자가 존재하지 않을 때만 생성
     */
    private void createUserIfNotExists(UserDto userDto, String userType) {
        try {
            // 이메일로 사용자 존재 여부 확인
            boolean exists = userService.getUserByEmailOptional(userDto.getEmail()).isPresent();
            
            if (!exists) {
                userService.createUser(userDto);
                log.info("{} 생성 완료: {}", userType, userDto.getEmail());
            } else {
                log.info("{}이(가) 이미 존재합니다: {}", userType, userDto.getEmail());
            }
        } catch (Exception e) {
            log.error("{} 생성 중 오류 발생: {} - {}", userType, userDto.getEmail(), e.getMessage());
        }
    }
} 