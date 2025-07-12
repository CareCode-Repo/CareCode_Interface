package com.carecode.domain.health.service;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.domain.health.dto.ChildDto;
import com.carecode.domain.health.dto.GrowthTrendDto;
import com.carecode.domain.health.dto.HealthRecordDto;
import com.carecode.domain.health.entity.HealthRecord;
import com.carecode.domain.health.repository.HealthRecordRepository;
import com.carecode.domain.user.repository.ChildRepository;
import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 건강 기록 서비스 클래스
 * 아동 건강 기록 관련 비즈니스 로직 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class HealthRecordService {

    private final HealthRecordRepository healthRecordRepository;
    private final ChildRepository childRepository;
    private final UserRepository userRepository;

    /**
     * 건강 기록 목록 조회
     */
    @LogExecutionTime
    public List<HealthRecordDto> getHealthRecords(Long childId, int page, int size) {
        log.info("건강 기록 목록 조회 - 아동 ID: {}", childId);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "recordDate"));
        Page<HealthRecord> records = healthRecordRepository.findByChildIdOrderByRecordDateDesc(childId, pageable);
        
        return records.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 건강 기록 상세 조회
     */
    @LogExecutionTime
    public HealthRecordDto getHealthRecordById(Long recordId) {
        log.info("건강 기록 상세 조회 - 기록 ID: {}", recordId);
        
        HealthRecord record = healthRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("건강 기록을 찾을 수 없습니다: " + recordId));
        
        return convertToDto(record);
    }

    /**
     * 건강 기록 생성
     */
    @Transactional
    public HealthRecordDto createHealthRecord(HealthRecordDto request) {
        log.info("건강 기록 생성 - 아동 ID: {}, 기록 날짜: {}", request.getChildId(), request.getRecordDate());
        
        Child child = childRepository.findById(request.getChildId())
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다: " + request.getChildId()));
        
        HealthRecord record = HealthRecord.builder()
                .child(child)
                .recordDate(request.getRecordDate())
                .height(request.getHeight())
                .weight(request.getWeight())
                .temperature(request.getTemperature())
                .bloodPressure(request.getBloodPressure())
                .pulseRate(request.getPulseRate())
                .notes(request.getNotes())
                .build();
        
        HealthRecord savedRecord = healthRecordRepository.save(record);
        return convertToDto(savedRecord);
    }

    /**
     * 건강 기록 수정
     */
    @Transactional
    public HealthRecordDto updateHealthRecord(Long recordId, HealthRecordDto request) {
        log.info("건강 기록 수정 - 기록 ID: {}", recordId);
        
        HealthRecord record = healthRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("건강 기록을 찾을 수 없습니다: " + recordId));
        
        record.updateRecord(
                request.getRecordDate(),
                request.getHeight(),
                request.getWeight(),
                request.getTemperature(),
                request.getBloodPressure(),
                request.getPulseRate(),
                request.getNotes()
        );
        
        HealthRecord updatedRecord = healthRecordRepository.save(record);
        return convertToDto(updatedRecord);
    }

    /**
     * 건강 기록 삭제
     */
    @Transactional
    public void deleteHealthRecord(Long recordId) {
        log.info("건강 기록 삭제 - 기록 ID: {}", recordId);
        
        healthRecordRepository.deleteById(recordId);
    }

    /**
     * 기간별 건강 기록 조회
     */
    @LogExecutionTime
    public List<HealthRecordDto> getHealthRecordsByDateRange(Long childId, LocalDate startDate, LocalDate endDate) {
        log.info("기간별 건강 기록 조회 - 아동 ID: {}, 시작일: {}, 종료일: {}", childId, startDate, endDate);
        
        List<HealthRecord> records = healthRecordRepository.findByChildIdAndRecordDateBetweenOrderByRecordDateDesc(
                childId, startDate, endDate);
        
        return records.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 성장 추이 조회
     */
    @LogExecutionTime
    public GrowthTrendDto getGrowthTrend(Long childId, int months) {
        log.info("성장 추이 조회 - 아동 ID: {}, 개월: {}", childId, months);
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(months);
        
        List<HealthRecord> records = healthRecordRepository.findByChildIdAndRecordDateBetweenOrderByRecordDateAsc(
                childId, startDate, endDate);
        
        return GrowthTrendDto.builder()
                .childId(childId)
                .startDate(startDate)
                .endDate(endDate)
                .heightTrend(records.stream().map(HealthRecord::getHeight).collect(Collectors.toList()))
                .weightTrend(records.stream().map(HealthRecord::getWeight).collect(Collectors.toList()))
                .build();
    }

    /**
     * 아동 정보 조회
     */
    @LogExecutionTime
    public ChildDto getChildById(Long childId) {
        log.info("아동 정보 조회 - 아동 ID: {}", childId);
        
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다: " + childId));
        
        return convertToChildDto(child);
    }

    /**
     * 사용자별 아동 목록 조회
     */
    @LogExecutionTime
    public List<ChildDto> getChildrenByUserId(Long userId) {
        log.info("사용자별 아동 목록 조회 - 사용자 ID: {}", userId);
        
        List<Child> children = childRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return children.stream()
                .map(this::convertToChildDto)
                .collect(Collectors.toList());
    }

    /**
     * 아동 정보 생성
     */
    @Transactional
    public ChildDto createChild(ChildDto request) {
        log.info("아동 정보 생성 - 사용자 ID: {}, 이름: {}", request.getUserId(), request.getName());
        
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + request.getUserId()));
        
        Child child = Child.builder()
                .user(user)
                .name(request.getName())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .build();
        
        Child savedChild = childRepository.save(child);
        return convertToChildDto(savedChild);
    }

    /**
     * Entity를 DTO로 변환
     */
    private HealthRecordDto convertToDto(HealthRecord record) {
        return HealthRecordDto.builder()
                .id(record.getId())
                .childId(record.getChild().getId())
                .recordDate(record.getRecordDate())
                .height(record.getHeight())
                .weight(record.getWeight())
                .temperature(record.getTemperature())
                .bloodPressure(record.getBloodPressure())
                .pulseRate(record.getPulseRate())
                .notes(record.getNotes())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    /**
     * Child Entity를 DTO로 변환
     */
    private ChildDto convertToChildDto(Child child) {
        return ChildDto.builder()
                .id(child.getId())
                .userId(child.getUser().getId())
                .name(child.getName())
                .birthDate(child.getBirthDate())
                .gender(child.getGender())
                .createdAt(child.getCreatedAt())
                .updatedAt(child.getUpdatedAt())
                .build();
    }
} 