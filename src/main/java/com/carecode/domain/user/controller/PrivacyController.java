package com.carecode.domain.user.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.util.ClientIpResolver;
import com.carecode.domain.user.dto.request.ConsentUpdateRequest;
import com.carecode.domain.user.dto.response.ConsentStatusResponse;
import com.carecode.domain.user.service.PrivacyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 개인정보 관련 API: 동의 관리, 내 데이터 열람, 회원 탈퇴.
 */
@RestController
@RequestMapping("/users/privacy")
@RequiredArgsConstructor
@Tag(name = "개인정보", description = "동의 이력, 데이터 열람 및 파기 API")
public class PrivacyController {

    private final PrivacyService privacyService;
    private final ClientIpResolver clientIpResolver;

    @GetMapping("/consents")
    @LogExecutionTime
    @Operation(summary = "동의 상태 조회", description = "항목별 현재 동의 여부와 동의한 약관 버전을 반환합니다.")
    public ResponseEntity<ConsentStatusResponse> getConsents() {
        return ResponseEntity.ok(privacyService.getConsentStatus());
    }

    @PostMapping("/consents")
    @LogExecutionTime
    @Operation(summary = "동의/철회 기록",
            description = "동의 이력은 덮어쓰지 않고 새 기록으로 남습니다.")
    public ResponseEntity<ConsentStatusResponse> updateConsent(
            @Valid @RequestBody ConsentUpdateRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(
                privacyService.recordConsent(request, clientIpResolver.resolve(httpRequest)));
    }

    @GetMapping("/consents/history")
    @LogExecutionTime
    @Operation(summary = "동의 이력 전체 조회")
    public ResponseEntity<List<ConsentStatusResponse.ConsentHistoryItem>> getConsentHistory() {
        return ResponseEntity.ok(privacyService.getConsentHistory());
    }

    @GetMapping("/export")
    @LogExecutionTime
    @Operation(summary = "내 데이터 내려받기",
            description = "개인정보 열람권 행사를 위한 데이터 export. 인증 정보는 포함하지 않습니다.")
    public ResponseEntity<Map<String, Object>> exportMyData() {
        return ResponseEntity.ok(privacyService.exportMyData());
    }

    @DeleteMapping("/account")
    @LogExecutionTime
    @Operation(summary = "회원 탈퇴",
            description = "개인 식별 정보를 익명화하고 계정을 비활성화합니다.")
    public ResponseEntity<Void> deleteAccount() {
        privacyService.deleteMyAccount();
        return ResponseEntity.noContent().build();
    }
}
