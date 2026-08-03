package com.carecode.domain.community.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.domain.community.dto.request.ReportCreateRequest;
import com.carecode.domain.community.dto.response.ReportResponse;
import com.carecode.domain.community.service.ModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 커뮤니티 신고·차단 API (사용자용).
 */
@RestController
@RequestMapping("/community")
@RequiredArgsConstructor
@Tag(name = "커뮤니티 모더레이션", description = "게시글·댓글 신고 및 사용자 차단 API")
public class ModerationController {

    private final ModerationService moderationService;

    @PostMapping("/reports")
    @LogExecutionTime
    @Operation(summary = "게시글·댓글 신고",
            description = "신고가 누적되면 관리자 확인 전까지 자동으로 숨김 처리됩니다.")
    public ResponseEntity<ReportResponse> report(@Valid @RequestBody ReportCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(moderationService.report(request));
    }

    @PostMapping("/blocks/{userId}")
    @LogExecutionTime
    @Operation(summary = "사용자 차단", description = "차단한 사용자의 글과 댓글이 목록에서 보이지 않습니다.")
    public ResponseEntity<Void> blockUser(@PathVariable Long userId) {
        moderationService.blockUser(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/blocks/{userId}")
    @LogExecutionTime
    @Operation(summary = "사용자 차단 해제")
    public ResponseEntity<Void> unblockUser(@PathVariable Long userId) {
        moderationService.unblockUser(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/blocks")
    @LogExecutionTime
    @Operation(summary = "내가 차단한 사용자 목록")
    public ResponseEntity<List<Long>> getBlockedUsers() {
        return ResponseEntity.ok(moderationService.getBlockedUserIds());
    }
}
