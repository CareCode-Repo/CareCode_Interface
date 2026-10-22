package com.carecode.domain.admin.controller;

import com.carecode.domain.admin.service.AdminCommunityService;
import com.carecode.domain.community.dto.response.CommunityPostResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 어드민 커뮤니티 관리 API. 관리자는 게시글을 대신 작성하지 않는다. 모더레이션(조회/숨김)만 제공한다. */
@RestController
@RequestMapping("/api/admin/community/posts")
@RequiredArgsConstructor
@Tag(name = "어드민 - 커뮤니티", description = "관리자 전용 게시글 모더레이션 API")
public class AdminCommunityController {

    private final AdminCommunityService adminCommunityService;

    @GetMapping
    @Operation(summary = "게시글 목록 조회")
    public ResponseEntity<Page<CommunityPostResponse>> list(
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adminCommunityService.list(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "게시글 상세 조회")
    public ResponseEntity<CommunityPostResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(adminCommunityService.detail(id));
    }

    /**
     * 게시글 숨김.
     *
     * <p>신고 수락과 같은 결과다 — 행과 댓글은 남고 목록에서만 빠진다. 하드 삭제하면 FK 의
     * {@code ON DELETE CASCADE} 로 댓글·좋아요·북마크까지 사라지고 되돌릴 수 없다.
     * 이 저장소가 사용자 탈퇴에 적용한 원칙과 같다.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "게시글 숨김",
            description = "부적절한 게시글을 목록에서 내립니다. 댓글·좋아요는 보존되고 되돌릴 수 있습니다")
    public ResponseEntity<Void> hide(@PathVariable Long id) {
        adminCommunityService.hide(id);
        return ResponseEntity.noContent().build();
    }
}
