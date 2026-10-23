package com.carecode.domain.health.service;

import com.carecode.core.exception.ChildNotFoundException;
import com.carecode.core.exception.HealthRecordNotFoundException;
import com.carecode.domain.health.dto.request.HealthCreateHealthRecordRequest;
import com.carecode.domain.health.dto.response.HealthRecordResponse;
import com.carecode.domain.health.entity.HealthRecordAttachment;
import com.carecode.core.storage.FileStorageService;
import com.carecode.domain.health.entity.HealthRecord;
import com.carecode.domain.health.mapper.HealthRecordMapper;
import com.carecode.domain.health.repository.HealthRecordAttachmentRepository;
import com.carecode.domain.health.repository.HealthRecordRepository;
import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.app.ChildDirectory;
import com.carecode.domain.user.repository.ChildRepository;
import com.carecode.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.carecode.domain.user.service.ConsentGuard;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** HealthService 단위 테스트 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HealthService 테스트")
class HealthServiceTest {

    @Mock
    private HealthRecordRepository healthRecordRepository;

    @Mock
    private ChildRepository childRepository;

    /** 소유권 판단은 이 입구가 한다. 목이 없으면 null 이 되어 NPE 가 CareServiceException 으로 덮인다. */
    @Mock
    private ChildDirectory childDirectory;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HealthRecordMapper healthRecordMapper;

    /** 동의 확인은 ConsentGuardTest 에서 검증한다. 여기서는 통과시킨 뒤 기록 로직만 본다. */
    @Mock
    private ConsentGuard consentGuard;

    /** 기록을 지우면 첨부 파일도 지워야 한다. 그 호출을 보기 위한 목이다. */
    @Mock
    private HealthRecordAttachmentRepository healthRecordAttachmentRepository;

    @Mock
    private FileStorageService fileStorageService;

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
        HealthRecordResponse response = healthService.createHealthRecord(testRequest, testUser.getId());

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
        assertThatThrownBy(() -> healthService.createHealthRecord(testRequest, testUser.getId()))
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
        HealthRecordResponse response = healthService.getHealthRecordById(recordId, testUser.getId());

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
        assertThatThrownBy(() -> healthService.getHealthRecordById(recordId, testUser.getId()))
                .isInstanceOf(HealthRecordNotFoundException.class)
                .hasMessageContaining("건강 기록을 찾을 수 없습니다");

        verify(healthRecordRepository).findById(recordId);
        verify(healthRecordMapper, never()).toResponse(any());
    }

    /**
     * 건강기록을 지우면 첨부 파일도 지워야 한다.
     *
     * <p>엔티티의 {@code cascade = ALL, orphanRemoval = true} 는 첨부 <b>행</b>만 지운다.
     * 저장된 파일은 디스크에 남는다. 진단서·처방전 같은 민감정보가 "지웠다" 고 생각한 뒤에도
     * 서버에 남아 있는 상태다.
     */
    @Test
    @DisplayName("기록을 지우면 첨부 파일도 지운다")
    void deletingRecordRemovesAttachmentFiles() {
        Long recordId = 1L;
        HealthRecord record = HealthRecord.builder().id(recordId).user(testUser).build();
        HealthRecordAttachment attachment = HealthRecordAttachment.builder()
                .healthRecord(record)
                .fileUrl("health-records/2026/10/23/stored.png")
                .fileName("진단서.png")
                .build();

        when(healthRecordRepository.findById(recordId)).thenReturn(java.util.Optional.of(record));
        when(healthRecordAttachmentRepository.findByHealthRecordIdOrderByDisplayOrderAsc(recordId))
                .thenReturn(java.util.List.of(attachment));
        when(fileStorageService.toKey(anyString())).thenAnswer(inv -> inv.getArgument(0));

        healthService.deleteHealthRecord(recordId, testUser.getId());

        verify(fileStorageService).delete("health-records/2026/10/23/stored.png");
        verify(healthRecordRepository).delete(record);
    }
}

