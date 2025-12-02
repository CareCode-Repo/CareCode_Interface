package com.carecode.domain.community.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.annotation.RequireAuthentication;
import com.carecode.core.controller.BaseController;
import com.carecode.domain.community.dto.request.response.CommunityRequest;
import com.carecode.domain.community.dto.request.response.CommunityCreatePostRequest;
import com.carecode.domain.community.dto.request.response.CommunityUpdatePostRequest;
import com.carecode.domain.community.dto.request.response.CommunityCreateCommentRequest;
import com.carecode.domain.community.dto.request.response.CommunityUpdateCommentRequest;
import com.carecode.domain.community.dto.response.CommunityResponse;
import com.carecode.domain.community.dto.response.CommunityPostResponse;
import com.carecode.domain.community.dto.response.CommunityPostDetailResponse;
import com.carecode.domain.community.dto.response.CommunityCommentResponse;
import com.carecode.domain.community.dto.response.CommunityTagResponse;
import com.carecode.domain.community.dto.response.CommunityPageResponse;
import com.carecode.domain.community.service.CommunityService;
import com.carecode.domain.community.app.CommunityFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import com.carecode.core.handler.ApiSuccess;
import java.util.Date;

/**
 * 커뮤니티 API 컨트롤러
 * 육아 커뮤니티 게시글 및 댓글 관리 서비스
 */
@RestController
@RequestMapping("/community")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*") // CORS 허용
@Tag(name = "커뮤니티", description = "육아 커뮤니티 게시글 및 댓글 관리 API")
public class CommunityController extends BaseController {

    private final CommunityFacade communityFacade;

    /**
     * 게시글 목록 조회 (페이징)
     */
    @GetMapping("/posts")
    @LogExecutionTime
    @Operation(summary = "게시글 목록 조회", description = "커뮤니티 게시글 목록을 페이징으로 조회합니다.")
    public ResponseEntity<CommunityPageResponse<CommunityPostResponse>> getAllPosts(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 항목 수", example = "10") @RequestParam(defaultValue = "10") int size) {
        CommunityPageResponse<CommunityPostResponse> posts = communityFacade.getAllPosts(page, size);
        return ResponseEntity.ok(posts);
    }

    /**
     * 게시글 상세 조회
     */
    @GetMapping("/posts/{postId}")
    @LogExecutionTime
    @Operation(summary = "게시글 상세 조회", description = "특정 게시글의 상세 정보를 조회합니다.")
    public ResponseEntity<CommunityPostDetailResponse> getPost(
            @Parameter(description = "게시글 ID", required = true) @PathVariable Long postId) {
        CommunityPostDetailResponse post = communityFacade.getPostDetailById(postId);
        return ResponseEntity.ok(post);
    }

    /**
     * 게시글 작성
     */
    @PostMapping("/posts")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "게시글 작성", description = "새로운 게시글을 작성합니다.")
    public ResponseEntity<CommunityPostResponse> createPost(
            @Parameter(description = "게시글 정보", required = true) @RequestBody CommunityCreatePostRequest request) {
        CommunityPostResponse post = communityFacade.createPost(request);
        return ResponseEntity.ok(post);
    }

    /**
     * 게시글 수정
     */
    @PutMapping("/posts/{postId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "게시글 수정", description = "기존 게시글을 수정합니다.")
    public ResponseEntity<CommunityPostResponse> updatePost(
            @Parameter(description = "게시글 ID", required = true) @PathVariable Long postId,
            @Parameter(description = "수정할 게시글 정보", required = true) @RequestBody CommunityUpdatePostRequest request) {
        CommunityPostResponse post = communityFacade.updatePost(postId, request);
        return ResponseEntity.ok(post);
    }

    /**
     * 게시글 삭제
     */
    @DeleteMapping("/posts/{postId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "게시글 삭제", description = "게시글을 삭제합니다.")
    public ResponseEntity<ApiSuccess> deletePost(
            @Parameter(description = "게시글 ID", required = true) @PathVariable Long postId) {
        communityFacade.deletePost(postId);
        return ResponseEntity.ok(ApiSuccess.builder().timestamp(new Date()).message("게시글이 삭제되었습니다.").build());
    }

    /**
     * 댓글 목록 조회
     */
    @GetMapping("/posts/{postId}/comments")
    @LogExecutionTime
    @Operation(summary = "댓글 목록 조회", description = "특정 게시글의 댓글 목록을 조회합니다.")
    public ResponseEntity<List<CommunityCommentResponse>> getComments(
            @Parameter(description = "게시글 ID", required = true) @PathVariable Long postId) {
        List<CommunityCommentResponse> comments = communityFacade.getCommentsByPostId(postId);
        return ResponseEntity.ok(comments);
    }

    /**
     * 댓글 작성
     */
    @PostMapping("/posts/{postId}/comments")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "댓글 작성", description = "게시글에 댓글을 작성합니다.")
    public ResponseEntity<CommunityCommentResponse> createComment(
            @Parameter(description = "게시글 ID", required = true) @PathVariable Long postId,
            @Parameter(description = "댓글 정보", required = true) @RequestBody CommunityCreateCommentRequest request) {
        CommunityCommentResponse comment = communityFacade.createComment(postId, request);
        return ResponseEntity.ok(comment);
    }

    /**
     * 댓글 수정
     */
    @PutMapping("/comments/{commentId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "댓글 수정", description = "기존 댓글을 수정합니다.")
    public ResponseEntity<CommunityCommentResponse> updateComment(
            @Parameter(description = "댓글 ID", required = true) @PathVariable Long commentId,
            @Parameter(description = "수정할 댓글 정보", required = true) @RequestBody CommunityUpdateCommentRequest request) {
        CommunityCommentResponse comment = communityFacade.updateComment(commentId, request);
        return ResponseEntity.ok(comment);
    }

    /**
     * 댓글 삭제
     */
    @DeleteMapping("/comments/{commentId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다.")
    public ResponseEntity<ApiSuccess> deleteComment(
            @Parameter(description = "댓글 ID", required = true) @PathVariable Long commentId) {
        communityFacade.deleteComment(commentId);
        return ResponseEntity.ok(ApiSuccess.builder().timestamp(new Date()).message("댓글이 삭제되었습니다.").build());
    }

    /**
     * 게시글 검색 (페이징)
     */
    @GetMapping("/search")
    @LogExecutionTime
    @Operation(summary = "게시글 검색", description = "키워드로 게시글을 페이징 검색합니다.")
    public ResponseEntity<CommunityPageResponse<CommunityPostResponse>> searchPosts(
            @Parameter(description = "검색 키워드", required = true) @RequestParam String keyword,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 항목 수", example = "10") @RequestParam(defaultValue = "10") int size) {
        CommunityPageResponse<CommunityPostResponse> posts = communityFacade.searchPosts(keyword, page, size);
        return ResponseEntity.ok(posts);
    }

    /**
     * 인기 게시글 조회 (페이징)
     */
    @GetMapping("/popular")
    @LogExecutionTime
    @Operation(summary = "인기 게시글 조회", description = "인기 있는 게시글 목록을 페이징으로 조회합니다.")
    public ResponseEntity<CommunityPageResponse<CommunityPostResponse>> getPopularPosts(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 항목 수", example = "10") @RequestParam(defaultValue = "10") int size) {
        CommunityPageResponse<CommunityPostResponse> posts = communityFacade.getPopularPosts(page, size);
        return ResponseEntity.ok(posts);
    }

    /**
     * 최신 게시글 조회 (페이징)
     */
    @GetMapping("/latest")
    @LogExecutionTime
    @Operation(summary = "최신 게시글 조회", description = "최근 작성된 게시글 목록을 페이징으로 조회합니다.")
    public ResponseEntity<CommunityPageResponse<CommunityPostResponse>> getLatestPosts(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 항목 수", example = "10") @RequestParam(defaultValue = "10") int size) {
        CommunityPageResponse<CommunityPostResponse> posts = communityFacade.getLatestPosts(page, size);
        return ResponseEntity.ok(posts);
    }

    /**
     * 태그 목록 조회
     */
    @GetMapping("/tags")
    @LogExecutionTime
    @Operation(summary = "태그 목록 조회", description = "커뮤니티 태그 목록을 조회합니다.")
    public ResponseEntity<List<CommunityTagResponse>> getAllTags() {
        List<CommunityTagResponse> tags = communityFacade.getAllTags();
        return ResponseEntity.ok(tags);
    }

} 