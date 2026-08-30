package com.carecode.domain.chatbot.controller;

import com.carecode.core.security.CurrentUserFacade;
import com.carecode.domain.chatbot.app.ChatbotFacade;
import com.carecode.domain.chatbot.entity.ChatMessage;
import com.carecode.domain.chatbot.entity.ChatSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 챗봇 조회가 요청 파라미터의 userId 를 믿지 않는지 고정한다.
 *
 * <p>이 컨트롤러의 조회 7종은 {@code @RequestParam String userId} 를 그대로 파사드에
 * 넘기고 있었다. 로그인만 하면 남의 userId 를 적어 <b>다른 사람의 상담 내역과 세션</b>을
 * 읽을 수 있었다는 뜻이다. 챗봇 대화에는 아이 건강·가정 사정이 담긴다.
 *
 * <p>같은 도메인의 HealthController·NotificationController 는 이미 클라이언트가 준 userId 를
 * 무시하고 인증 주체를 쓴다. 여기만 빠져 있었다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ChatbotController - 조회 대상은 인증 주체")
class ChatbotControllerOwnershipTest {

    private static final String ME = "user_me";
    private static final String VICTIM = "user_victim";

    @Mock private ChatbotFacade chatbotFacade;
    @Mock private CurrentUserFacade currentUserFacade;

    @InjectMocks private ChatbotController controller;

    @BeforeEach
    void setUp() {
        when(currentUserFacade.requireCurrentUserId()).thenReturn(ME);
    }

    @Test
    @DisplayName("의도별 메시지 조회는 남의 userId 를 무시한다")
    void intentSearchIgnoresRequestedUserId() {
        controller.getMessagesByIntentType(VICTIM, "QUESTION");

        verify(chatbotFacade).getMessagesByIntentType(eq(ME), any(ChatMessage.IntentType.class));
        verify(chatbotFacade, never()).getMessagesByIntentType(eq(VICTIM), any());
    }

    @Test
    @DisplayName("기간별 메시지 조회는 남의 userId 를 무시한다")
    void dateRangeSearchIgnoresRequestedUserId() {
        controller.getMessagesByDateRange(VICTIM, "2026-01-01T00:00:00", "2026-12-31T00:00:00");

        verify(chatbotFacade).getMessagesByDateRange(eq(ME), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("도움됨 여부별 조회는 남의 userId 를 무시한다")
    void helpfulSearchIgnoresRequestedUserId() {
        controller.getMessagesByHelpfulStatus(VICTIM, true);

        verify(chatbotFacade).getMessagesByHelpfulStatus(ME, true);
        verify(chatbotFacade, never()).getMessagesByHelpfulStatus(eq(VICTIM), any());
    }

    @Test
    @DisplayName("키워드 검색은 남의 userId 를 무시한다")
    void keywordSearchIgnoresRequestedUserId() {
        when(chatbotFacade.searchMessagesByKeyword(any(), any())).thenReturn(List.of());

        controller.searchMessagesByKeyword(VICTIM, "예방접종");

        verify(chatbotFacade).searchMessagesByKeyword(ME, "예방접종");
    }

    @Test
    @DisplayName("상태별 세션 조회는 남의 userId 를 무시한다")
    void sessionsByStatusIgnoreRequestedUserId() {
        controller.getSessionsByStatus(VICTIM, "ACTIVE");

        verify(chatbotFacade).getSessionsByStatus(eq(ME), any(ChatSession.SessionStatus.class));
    }

    @Test
    @DisplayName("기간별 세션 조회는 남의 userId 를 무시한다")
    void sessionsByDateRangeIgnoreRequestedUserId() {
        controller.getSessionsByDateRange(VICTIM, "2026-01-01T00:00:00", "2026-12-31T00:00:00");

        verify(chatbotFacade).getSessionsByDateRange(eq(ME), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("세션 수 조회는 남의 userId 를 무시한다")
    void sessionCountIgnoresRequestedUserId() {
        controller.getSessionCountByUser(VICTIM);

        verify(chatbotFacade).getSessionCountByUser(ME);
        verify(chatbotFacade, never()).getSessionCountByUser(VICTIM);
    }

    @Test
    @DisplayName("userId 를 아예 보내지 않아도 동작한다")
    void userIdParameterIsOptional() {
        // 파라미터는 호환을 위해 남겨둔 것이라 없어도 된다.
        controller.getSessionCountByUser(null);

        verify(chatbotFacade).getSessionCountByUser(ME);
    }
}
