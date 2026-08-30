package com.carecode.domain.health.service;

import com.carecode.core.exception.HealthRecordNotFoundException;
import com.carecode.core.exception.ResourceNotFoundException;
import com.carecode.core.security.CurrentUserFacade;
import com.carecode.core.storage.FileStorageService;
import com.carecode.domain.health.dto.response.AttachmentDownload;
import com.carecode.domain.health.entity.HealthRecord;
import com.carecode.domain.health.entity.HealthRecordAttachment;
import com.carecode.domain.health.repository.HealthRecordAttachmentRepository;
import com.carecode.domain.health.repository.HealthRecordRepository;
import com.carecode.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 첨부파일 내려받기의 접근 제어 테스트.
 *
 * <p>건강기록 첨부는 진단서·검진표다. 업로드 저장소를 정적 경로로 공개하면 주소만 아는
 * 사람이 남의 진료 기록을 볼 수 있어, 소유권을 확인하고 서버가 직접 내려주는 경로를 쓴다.
 * 그 확인이 빠지면 그대로 개인정보 유출이므로 여기서 계약을 고정한다.
 *
 * <p>응답 코드도 함께 고정한다. "없는 첨부" 와 "남의 기록에 달린 첨부" 가 다른 코드를 주면
 * 그 차이만으로 id 의 존재 여부를 알아낼 수 있다. 둘 다 404 여야 한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("HealthRecordAttachmentService - 첨부 내려받기")
class HealthRecordAttachmentServiceTest {

    private static final Long MY_RECORD = 1L;
    private static final Long OTHERS_RECORD = 2L;

    @Mock private HealthRecordRepository healthRecordRepository;
    @Mock private HealthRecordAttachmentRepository attachmentRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private CurrentUserFacade currentUserFacade;

    @InjectMocks private HealthRecordAttachmentService service;

    private User me;

    @BeforeEach
    void setUp() {
        me = User.builder().id(10L).userId("user_me").email("me@example.com").build();
        User other = User.builder().id(99L).userId("user_other").email("other@example.com").build();

        when(currentUserFacade.requireCurrentUser()).thenReturn(me);

        when(healthRecordRepository.findById(MY_RECORD))
                .thenReturn(Optional.of(HealthRecord.builder().id(MY_RECORD).user(me).build()));
        when(healthRecordRepository.findById(OTHERS_RECORD))
                .thenReturn(Optional.of(HealthRecord.builder().id(OTHERS_RECORD).user(other).build()));

        when(fileStorageService.toKey(anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(fileStorageService.load(anyString()))
                .thenReturn(new ByteArrayResource("bytes".getBytes()));
    }

    private HealthRecordAttachment attachment(Long id, Long recordId) {
        HealthRecordAttachment attachment = HealthRecordAttachment.builder()
                .healthRecord(HealthRecord.builder().id(recordId).user(me).build())
                .fileUrl("health-records/2026/08/29/stored.png")
                .fileName("진단서.png")
                .fileType("image/png")
                .build();
        // id 는 @Builder 가 감싼 생성자에 없다(JPA 가 채우는 값). 테스트에서만 직접 넣는다.
        ReflectionTestUtils.setField(attachment, "id", id);
        return attachment;
    }

    @Nested
    @DisplayName("정상 경로")
    class Success {

        @Test
        @DisplayName("본인 기록의 첨부는 내려받을 수 있다")
        void ownerCanDownload() {
            when(attachmentRepository.findById(5L)).thenReturn(Optional.of(attachment(5L, MY_RECORD)));

            AttachmentDownload download = service.download(MY_RECORD, 5L);

            assertThat(download.getResource()).isNotNull();
            assertThat(download.getFileName()).isEqualTo("진단서.png");
            assertThat(download.getContentType()).isEqualTo("image/png");
        }

        @Test
        @DisplayName("저장 키는 공개 URL 을 되돌려 얻는다")
        void resolvesStorageKeyFromPublicUrl() {
            when(attachmentRepository.findById(5L)).thenReturn(Optional.of(attachment(5L, MY_RECORD)));

            service.download(MY_RECORD, 5L);

            verify(fileStorageService).toKey("health-records/2026/08/29/stored.png");
        }
    }

    @Nested
    @DisplayName("접근 제어")
    class AccessControl {

        @Test
        @DisplayName("남의 건강기록이면 404 이고 파일을 읽지 않는다")
        void othersRecordIsRejected() {
            assertThatThrownBy(() -> service.download(OTHERS_RECORD, 5L))
                    .isInstanceOf(HealthRecordNotFoundException.class);

            // 소유권 확인이 저장소 접근보다 먼저여야 한다.
            verify(fileStorageService, never()).load(anyString());
        }

        @Test
        @DisplayName("다른 기록에 달린 첨부 id 를 끼워 넣어도 받을 수 없다")
        void crossRecordAttachmentIsRejected() {
            // 내 기록 id 는 맞지만, 그 첨부는 다른 기록의 것이다.
            when(attachmentRepository.findById(7L)).thenReturn(Optional.of(attachment(7L, 12345L)));

            assertThatThrownBy(() -> service.download(MY_RECORD, 7L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(fileStorageService, never()).load(anyString());
        }

        @Test
        @DisplayName("없는 첨부와 남의 첨부는 같은 예외를 낸다")
        void missingAndForeignAreIndistinguishable() {
            when(attachmentRepository.findById(8L)).thenReturn(Optional.empty());
            when(attachmentRepository.findById(9L)).thenReturn(Optional.of(attachment(9L, 12345L)));

            // 응답이 갈리면 그 차이만으로 "그 id 는 존재한다" 를 알아낼 수 있다.
            assertThatThrownBy(() -> service.download(MY_RECORD, 8L))
                    .isInstanceOf(ResourceNotFoundException.class);
            assertThatThrownBy(() -> service.download(MY_RECORD, 9L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("건강기록이 없으면 404")
        void missingRecord() {
            when(healthRecordRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.download(404L, 5L))
                    .isInstanceOf(HealthRecordNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("삭제")
    class Delete {

        @Test
        @DisplayName("다른 기록의 첨부는 지울 수 없고 404 다")
        void crossRecordDeleteIsRejected() {
            when(attachmentRepository.findById(7L)).thenReturn(Optional.of(attachment(7L, 12345L)));

            assertThatThrownBy(() -> service.delete(MY_RECORD, 7L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(attachmentRepository, never()).delete(org.mockito.ArgumentMatchers.any());
        }
    }
}
