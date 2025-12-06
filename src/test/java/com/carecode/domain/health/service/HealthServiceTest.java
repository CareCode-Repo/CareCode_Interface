package com.carecode.domain.health.service;

import com.carecode.core.exception.ChildNotFoundException;
import com.carecode.core.exception.HealthRecordNotFoundException;
import com.carecode.domain.health.dto.request.HealthCreateHealthRecordRequest;
import com.carecode.domain.health.dto.response.HealthRecordResponse;
import com.carecode.domain.health.entity.HealthRecord;
import com.carecode.domain.health.mapper.HealthRecordMapper;
import com.carecode.domain.health.repository.HealthRecordRepository;
import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.ChildRepository;
import com.carecode.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * HealthService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HealthService 테스트")
class HealthServiceTest {

    @Mock
    private HealthRecordRepository healthRecordRepository;

    @Mock
    private ChildRepository childRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HealthRecordMapper healthRecordMapper;

    @InjectMocks
    private HealthService healthService;

    private User testUser;
    private Child testChild;
    private HealthRecord testHealthRecord;
    private HealthCreateHealthRecordRequest testRequest;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        testUser = User.builder()
                .id(1L)
                .userId("user123")
                .email("test@example.com")
                .name("테스트 사용자")
                .build();

        testChild = Child.builder()
                .id(1L)
                .user(testUser)
                .name("테스트 아동")
                .birthDate(LocalDate.of(2020, 1, 1))
                .build();

        testHealthRecord = HealthRecord.builder()
                .id(1L)
                .child(testChild)
                .user(testUser)
                .title("예방접종")
                .description("접종 내용")
                .recordDate(LocalDate.now())
                .build();

        testRequest = new HealthCreateHealthRecordRequest();
        testRequest.setChildId("1");
        testRequest.setTitle("예방접종");
        testRequest.setDescription("접종 내용");
    }

    @Test
    @DisplayName("건강 기록 생성 성공")
    void createHealthRecord_ShouldReturnResponse_WhenValidRequest() {
        // Given
        when(childRepository.findById(1L)).thenReturn(Optional.of(testChild));
        when(healthRecordMapper.toEntity(testRequest)).thenReturn(testHealthRecord);
        when(healthRecordRepository.save(any(HealthRecord.class))).thenReturn(testHealthRecord);
        when(healthRecordMapper.toResponse(testHealthRecord)).thenReturn(
                HealthRecordResponse.builder()
                        .id(1L)
                        .title("예방접종")
                        .build()
        );

        // When
        HealthRecordResponse response = healthService.createHealthRecord(testRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("예방접종");
        
        verify(childRepository).findById(1L);
        verify(healthRecordRepository).save(any(HealthRecord.class));
        verify(healthRecordMapper).toResponse(testHealthRecord);
    }

    @Test
    @DisplayName("건강 기록 생성 실패 - 아동을 찾을 수 없음")
    void createHealthRecord_ShouldThrowException_WhenChildNotFound() {
        // Given
        when(childRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> healthService.createHealthRecord(testRequest))
                .isInstanceOf(ChildNotFoundException.class)
                .hasMessageContaining("아동을 찾을 수 없습니다");

        verify(childRepository).findById(1L);
        verify(healthRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("건강 기록 조회 성공")
    void getHealthRecordById_ShouldReturnResponse_WhenRecordExists() {
        // Given
        Long recordId = 1L;
        when(healthRecordRepository.findById(recordId)).thenReturn(Optional.of(testHealthRecord));
        when(healthRecordMapper.toResponse(testHealthRecord)).thenReturn(
                HealthRecordResponse.builder()
                        .id(recordId)
                        .title("예방접종")
                        .build()
        );

        // When
        HealthRecordResponse response = healthService.getHealthRecordById(recordId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(recordId);
        
        verify(healthRecordRepository).findById(recordId);
        verify(healthRecordMapper).toResponse(testHealthRecord);
    }

    @Test
    @DisplayName("건강 기록 조회 실패 - 기록을 찾을 수 없음")
    void getHealthRecordById_ShouldThrowException_WhenRecordNotFound() {
        // Given
        Long recordId = 999L;
        when(healthRecordRepository.findById(recordId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> healthService.getHealthRecordById(recordId))
                .isInstanceOf(HealthRecordNotFoundException.class)
                .hasMessageContaining("건강 기록을 찾을 수 없습니다");

        verify(healthRecordRepository).findById(recordId);
        verify(healthRecordMapper, never()).toResponse(any());
    }
}

