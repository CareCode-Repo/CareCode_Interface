package com.carecode.domain.health.service;

import com.carecode.domain.health.entity.HealthRecord;
import com.carecode.domain.health.repository.HealthRecordRepository;
import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.ChildRepository;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 건강 기록 초기화 서비스
 * 애플리케이션 시작 시 테스트용 건강 기록 데이터 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthInitializationService implements CommandLineRunner {

    private final HealthRecordRepository healthRecordRepository;
    private final UserRepository userRepository;
    private final ChildRepository childRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        createTestHealthRecords();
    }

    /**
     * 테스트용 건강 기록 생성
     */
    private void createTestHealthRecords() {
        try {
            // 사용자 조회 (첫 번째 사용자 사용)
            User user = userRepository.findAll().stream().findFirst().orElse(null);
            if (user == null) {
                log.warn("테스트 사용자가 없어서 건강 기록을 생성할 수 없습니다.");
                return;
            }

            // 아동 조회 (첫 번째 아동 사용)
            Child child = childRepository.findAll().stream().findFirst().orElse(null);
            if (child == null) {
                log.warn("테스트 아동이 없어서 건강 기록을 생성할 수 없습니다.");
                return;
            }

            // 이미 데이터가 있으면 생성하지 않음
            if (healthRecordRepository.count() > 0) {
                log.info("건강 기록이 이미 존재합니다. 테스트 데이터 생성을 건너뜁니다.");
                return;
            }

            List<HealthRecord> records = Arrays.asList(
                // 예방접종 기록
                createHealthRecord(user, child, HealthRecord.RecordType.VACCINATION, 
                    "BCG 예방접종", "결핵 예방접종", LocalDate.now().minusMonths(2), 
                    LocalDate.now().plusMonths(10), "서울아동병원", "김의사", true),
                
                // 건강검진 기록
                createHealthRecord(user, child, HealthRecord.RecordType.CHECKUP, 
                    "1세 건강검진", "정기 건강검진", LocalDate.now().minusMonths(1), 
                    LocalDate.now().plusMonths(11), "서울아동병원", "이의사", true),
                
                // 성장기록
                createHealthRecord(user, child, HealthRecord.RecordType.GROWTH, 
                    "성장 측정", "키와 몸무게 측정", LocalDate.now().minusWeeks(2), 
                    null, "서울아동병원", "박의사", true),
                
                // 치과 검진
                createHealthRecord(user, child, HealthRecord.RecordType.DENTAL, 
                    "치과 검진", "치아 상태 확인", LocalDate.now().minusWeeks(1), 
                    LocalDate.now().plusMonths(6), "서울치과", "최치과의사", false),
                
                // 안과 검진
                createHealthRecord(user, child, HealthRecord.RecordType.EYE, 
                    "안과 검진", "시력 검사", LocalDate.now().minusDays(5), 
                    LocalDate.now().plusMonths(12), "서울안과", "정안과의사", false)
            );

            healthRecordRepository.saveAll(records);
            log.info("건강 기록 테스트 데이터 생성 완료: {}개", records.size());

        } catch (Exception e) {
            log.error("건강 기록 테스트 데이터 생성 중 오류 발생: {}", e.getMessage());
        }
    }

    /**
     * 건강 기록 생성 헬퍼 메서드
     */
    private HealthRecord createHealthRecord(User user, Child child, HealthRecord.RecordType recordType, 
                                          String title, String description, LocalDate recordDate, 
                                          LocalDate nextDate, String location, String doctorName, boolean isCompleted) {
        return HealthRecord.builder()
                .user(user)
                .child(child)
                .recordType(recordType)
                .title(title)
                .description(description)
                .recordDate(recordDate)
                .nextDate(nextDate)
                .location(location)
                .doctorName(doctorName)
                .isCompleted(isCompleted)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
} 