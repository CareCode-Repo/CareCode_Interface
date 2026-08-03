package com.carecode.domain.chatbot.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.carecode.domain.chatbot.rag.RetrievedContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Claude API 기반 챗봇 응답 생성기.
 *
 * <p>API 키가 없으면 비활성 상태로 동작하고, 호출부가 기존 룰 기반 응답으로 폴백한다.
 * 로컬/CI 환경에서 키 없이도 애플리케이션이 뜨도록 하기 위함이다.
 */
@Slf4j
@Component
public class ClaudeChatCompletionClient implements ChatCompletionClient {

    private static final String SYSTEM_PROMPT = """
            당신은 육아 지원 플랫폼 '맘편한'의 상담 도우미입니다.

            답변 규칙:
            - 아래 <참고자료>에 있는 내용을 우선 근거로 삼아 답하세요.
            - 참고자료에 없는 내용은 지어내지 말고, 모른다고 말한 뒤 관련 메뉴를 안내하세요.
            - 의료적 판단(진단, 투약, 응급 여부)은 하지 말고 전문의 상담을 권하세요.
            - 한국어로, 3~5문장 이내로 간결하게 답하세요.
            - 정책의 금액이나 신청 방법을 말할 때는 참고자료의 값을 그대로 인용하세요.
            """;

    private final AnthropicClient client;
    private final String model;
    private final long maxTokens;

    public ClaudeChatCompletionClient(
            @Value("${app.chatbot.claude.api-key:}") String apiKey,
            @Value("${app.chatbot.claude.model:claude-opus-5}") String model,
            @Value("${app.chatbot.claude.max-tokens:1024}") long maxTokens) {

        this.model = model;
        this.maxTokens = maxTokens;

        if (apiKey == null || apiKey.isBlank()) {
            log.info("Claude API 키가 설정되지 않아 챗봇이 룰 기반 응답으로 동작합니다. "
                    + "(app.chatbot.claude.api-key)");
            this.client = null;
        } else {
            this.client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
            log.info("Claude 챗봇 활성화 - model={}", model);
        }
    }

    @Override
    public boolean isAvailable() {
        return client != null;
    }

    @Override
    public Optional<String> generateReply(String userMessage, RetrievedContext context) {
        if (!isAvailable()) {
            return Optional.empty();
        }

        try {
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(model)
                    .maxTokens(maxTokens)
                    .system(SYSTEM_PROMPT)
                    .addUserMessage(buildUserPrompt(userMessage, context))
                    .build();

            Message response = client.messages().create(params);

            String text = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(textBlock -> textBlock.text())
                    .collect(Collectors.joining("\n"))
                    .trim();

            return text.isEmpty() ? Optional.empty() : Optional.of(text);
        } catch (Exception e) {
            // LLM 장애가 챗봇 전체를 막지 않도록 폴백을 허용한다.
            log.error("Claude 응답 생성 실패", e);
            return Optional.empty();
        }
    }

    /**
     * 검색된 근거를 프롬프트에 넣는다.
     * 근거가 없으면 그 사실을 명시해 모델이 없는 정보를 지어내지 않게 한다.
     */
    private String buildUserPrompt(String userMessage, RetrievedContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("<참고자료>\n");

        if (context == null || context.isEmpty()) {
            sb.append("(관련 등록 정보를 찾지 못했습니다. 일반적인 안내만 하고, 구체적인 정책명이나 시설명은 언급하지 마세요.)\n");
        } else {
            for (RetrievedContext.Snippet snippet : context.getSnippets()) {
                sb.append("- [").append(snippet.getSource()).append("] ")
                        .append(snippet.getTitle()).append(": ")
                        .append(snippet.getContent()).append('\n');
            }
        }

        sb.append("</참고자료>\n\n질문: ").append(userMessage);
        return sb.toString();
    }
}
