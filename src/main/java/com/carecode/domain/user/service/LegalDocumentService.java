package com.carecode.domain.user.service;

import com.carecode.core.exception.CareServiceException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 약관·개인정보 처리방침 원문 제공.
 * 동의 이력에 버전을 남기고 있는데 정작 그 버전의 원문을 볼 수 없으면 증빙이 되지 않는다.
 */
@Slf4j
@Service
public class LegalDocumentService {

    /** 현재 시행 중인 버전. 문서를 고치면 반드시 함께 올린다. */
    public static final String CURRENT_VERSION = "v1.0";

    private static final Map<DocumentType, String> PATHS = Map.of(
            DocumentType.PRIVACY_POLICY, "legal/privacy-policy-%s.md",
            DocumentType.TERMS_OF_SERVICE, "legal/terms-of-service-%s.md");

    /** 문서를 매 요청마다 읽지 않도록 캐시한다. 파일은 배포 단위로 고정이다. */
    private final Map<String, String> cache = new LinkedHashMap<>();

    @Getter
    public enum DocumentType {
        PRIVACY_POLICY("개인정보 처리방침"),
        TERMS_OF_SERVICE("서비스 이용약관");

        private final String displayName;

        DocumentType(String displayName) {
            this.displayName = displayName;
        }
    }

    public String getContent(DocumentType type, String version) {
        String resolved = version == null || version.isBlank() ? CURRENT_VERSION : version;
        String key = type.name() + ":" + resolved;

        return cache.computeIfAbsent(key, k -> load(type, resolved));
    }

    private String load(DocumentType type, String version) {
        String path = String.format(PATHS.get(type), version);
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("법적 고지 문서를 읽지 못했습니다: {}", path, e);
            throw new CareServiceException(
                    type.getDisplayName() + " " + version + " 문서를 찾을 수 없습니다.");
        }
    }
}
