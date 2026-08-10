package com.carecode.domain.user.controller;

import com.carecode.domain.user.service.LegalDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 약관·개인정보 처리방침 조회. 동의 전에 읽어야 하므로 비로그인도 접근할 수 있다. */
@RestController
@RequestMapping("/legal")
@RequiredArgsConstructor
@Tag(name = "약관·방침", description = "이용약관 및 개인정보 처리방침")
public class LegalDocumentController {

    private final LegalDocumentService legalDocumentService;

    @GetMapping(value = "/privacy-policy", produces = MediaType.TEXT_MARKDOWN_VALUE + ";charset=UTF-8")
    @Operation(summary = "개인정보 처리방침", description = "동의 화면에 표시할 원문")
    public ResponseEntity<String> privacyPolicy(
            @Parameter(description = "버전 (미지정 시 현재 시행본)") @RequestParam(required = false) String version) {
        return ResponseEntity.ok(legalDocumentService.getContent(
                LegalDocumentService.DocumentType.PRIVACY_POLICY, version));
    }

    @GetMapping(value = "/terms", produces = MediaType.TEXT_MARKDOWN_VALUE + ";charset=UTF-8")
    @Operation(summary = "서비스 이용약관", description = "동의 화면에 표시할 원문")
    public ResponseEntity<String> terms(
            @RequestParam(required = false) String version) {
        return ResponseEntity.ok(legalDocumentService.getContent(
                LegalDocumentService.DocumentType.TERMS_OF_SERVICE, version));
    }

    @GetMapping("/version")
    @Operation(summary = "현재 시행 버전", description = "동의 이력에 기록할 버전")
    public ResponseEntity<Map<String, String>> currentVersion() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("version", LegalDocumentService.CURRENT_VERSION);
        return ResponseEntity.ok(body);
    }
}
