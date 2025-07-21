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
import java.util.Optional;

/**
 * 건강 기록 초기화 서비스
 * 애플리케이션 시작 시 테스트용 건강 기록을 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthInitializationService implements CommandLineRunner {

    private final HealthRecordRepository healthRecordRepository;
    private final UserRepository userRepository;
    private final ChildRepository childRepository;

    @Override
    public void run(String... args) throws Exception {
        createTestHealthRecords();
    }

    /**
     * 테스트용 건강 기록 생성
     */
    @Transactional
    public void createTestHealthRecords() {
        try {
            // 테스트 사용자 조회
            Optional<User> testUser = userRepository.findByEmail("test1@carecode.com");
            if (testUser.isEmpty()) {
                log.info("테스트 사용자가 없어 건강 기록을 생성하지 않습니다.");
                return;
            }

            User user = testUser.get();
            
            // 테스트 아동 생성 또는 조회
            Child child = createOrGetTestChild(user);
            
            // 기존 건강 기록이 있는지 확인
            if (healthRecordRepository.countByChild(child) > 0) {
                log.info("이미 건강 기록이 존재합니다. 추가 생성하지 않습니다.");
                return;
            }

            // 테스트 건강 기록 생성
            createVaccinationRecord(child, user);
            createCheckupRecord(child, user);
            createGrowthRecord(child, user);

            log.info("테스트 건강 기록 생성 완료");

        } catch (Exception e) {
            log.error("테스트 건강 기록 생성 중 오류 발생: {}", e.getMessage());
        }
    }

    /**
     * 테스트 아동 생성 또는 조회
     */
    private Child createOrGetTestChild(User user) {
        // 기존 아동 조회
        Optional<Child> existingChild = childRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .findFirst();

        if (existingChild.isPresent()) {
            return existingChild.get();
        }

        // 새 아동 생성
        Child child = Child.builder()
                .user(user)
                .name("테스트 아동")
                .birthDate(LocalDate.of(2020, 1, 1))
                .gender("MALE")
                .build();

        return childRepository.save(child);
    }

    /**
     * 예방접종 기록 생성
     */
    private void createVaccinationRecord(Child child, User user) {
        HealthRecord vaccinationRecord = HealthRecord.builder()
                .child(child)
                .user(user)
                .recordType(HealthRecord.RecordType.VACCINATION)
                .title("BCG 예방접종")
                .description("BCG 예방접종 완료")
                .recordDate(LocalDate.of(2024, 1, 15))
                .nextDate(LocalDate.of(2024, 2, 15))
                .location("서울아동병원")
                .doctorName("김의사")
                .vaccineName("BCG")
                .vaccineBatch("BCG-2024-001")
                .isCompleted(true)
                .build();

        healthRecordRepository.save(vaccinationRecord);
        log.info("예방접종 기록 생성 완료: {}", vaccinationRecord.getTitle());
    }

    /**
     * 건강검진 기록 생성
     */
    private void createCheckupRecord(Child child, User user) {
        HealthRecord checkupRecord = HealthRecord.builder()
                .child(child)
                .user(user)
                .recordType(HealthRecord.RecordType.CHECKUP)
                .title("정기 건강검진")
                .description("1세 정기 건강검진 완료")
                .recordDate(LocalDate.of(2024, 2, 1))
                .nextDate(LocalDate.of(2024, 8, 1))
                .location("서울아동병원")
                .doctorName("박의사")
                .height(75.5)
                .weight(9.2)
                .temperature(36.8)
                .bloodPressure("90/60")
                .pulseRate(120)
                .diagnosis("정상 발달")
                .notes("키와 몸무게가 정상 범위입니다.")
                .isCompleted(true)
                .build();

        healthRecordRepository.save(checkupRecord);
        log.info("건강검진 기록 생성 완료: {}", checkupRecord.getTitle());
    }

    /**
     * 성장 기록 생성
     */
    private void createGrowthRecord(Child child, User user) {
        HealthRecord growthRecord = HealthRecord.builder()
                .child(child)
                .user(user)
                .recordType(HealthRecord.RecordType.GROWTH)
                .title("성장 기록")
                .description("월별 성장 측정")
                .recordDate(LocalDate.of(2024, 3, 1))
                .location("가정")
                .height(78.0)
                .weight(10.5)
                .temperature(36.7)
                .notes("이번 달 키 2.5cm, 몸무게 1.3kg 증가")
                .isCompleted(true)
                .build();

        healthRecordRepository.save(growthRecord);
        log.info("성장 기록 생성 완료: {}", growthRecord.getTitle());
    }
} 