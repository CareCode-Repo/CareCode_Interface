package com.carecode.domain.community.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.annotation.RequireAuthentication;
import com.carecode.core.controller.BaseController;
import com.carecode.core.exception.CareServiceException;
import com.carecode.domain.community.dto.CommunityRequestDto;
import com.carecode.domain.community.dto.CommunityResponseDto;
import com.carecode.domain.community.service.CommunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 커뮤니티 API 컨트롤러
 * 육아 커뮤니티 게시글 및 댓글 관리 서비스
 */
@RestController
@RequestMapping("/community")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "커뮤니티", description = "육아 커뮤니티 게시글 및 댓글 관리 API")
public class CommunityController extends BaseController {

    private final CommunityService communityService;

    /**
     * 게시글 목록 조회
     */
    @GetMapping("/posts")
    @LogExecutionTime
    @Operation(summary = "게시글 목록 조회", description = "커뮤니티 게시글 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = CommunityResponseDto.PostResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<CommunityResponseDto.PostResponse>> getAllPosts() {
        log.info("게시글 목록 조회");
        List<CommunityResponseDto.PostResponse> posts = communityService.getAllPosts();
        return ResponseEntity.ok(posts);
    }

    /**
     * 게시글 상세 조회
     */
    @GetMapping("/posts/{postId}")
    @LogExecutionTime
    @Operation(summary = "게시글 상세 조회", description = "특정 게시글의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = CommunityResponseDto.PostDetailResponse.class))),
        @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<CommunityResponseDto.PostDetailResponse> getPost(
            @Parameter(description = "게시글 ID", required = true) @PathVariable Long postId) {
        log.info("게시글 상세 조회: 게시글ID={}", postId);
        CommunityResponseDto.PostDetailResponse post = communityService.getPostById(postId);
        return ResponseEntity.ok(post);
    }

    /**
     * 게시글 작성
     */
    @PostMapping("/posts")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "게시글 작성", description = "새로운 게시글을 작성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "작성 성공",
            content = @Content(schema = @Schema(implementation = CommunityResponseDto.PostResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<CommunityResponseDto.PostResponse> createPost(
            @Parameter(description = "게시글 정보", required = true) @RequestBody CommunityRequestDto.CreatePostRequest request) {
        log.info("게시글 작성: 제목={}", request.getTitle());
        CommunityResponseDto.PostResponse post = communityService.createPost(request);
        return ResponseEntity.ok(post);
    }

    /**
     * 게시글 수정
     */
    @PutMapping("/posts/{postId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "게시글 수정", description = "기존 게시글을 수정합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "수정 성공",
            content = @Content(schema = @Schema(implementation = CommunityResponseDto.PostResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<CommunityResponseDto.PostResponse> updatePost(
            @Parameter(description = "게시글 ID", required = true) @PathVariable Long postId,
            @Parameter(description = "수정할 게시글 정보", required = true) @RequestBody CommunityRequestDto.UpdatePostRequest request) {
        log.info("게시글 수정: 게시글ID={}", postId);
        CommunityResponseDto.PostResponse post = communityService.updatePost(postId, request);
        return ResponseEntity.ok(post);
    }

    /**
     * 게시글 삭제
     */
    @DeleteMapping("/posts/{postId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "게시글 삭제", description = "게시글을 삭제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, String>> deletePost(
            @Parameter(description = "게시글 ID", required = true) @PathVariable Long postId) {
        log.info("게시글 삭제: 게시글ID={}", postId);
        communityService.deletePost(postId);
        return ResponseEntity.ok(Map.of("message", "게시글이 삭제되었습니다."));
    }

    /**
     * 댓글 목록 조회
     */
    @GetMapping("/posts/{postId}/comments")
    @LogExecutionTime
    @Operation(summary = "댓글 목록 조회", description = "특정 게시글의 댓글 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = CommunityResponseDto.CommentResponse.class))),
        @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<CommunityResponseDto.CommentResponse>> getComments(
            @Parameter(description = "게시글 ID", required = true) @PathVariable Long postId) {
        log.info("댓글 목록 조회: 게시글ID={}", postId);
        List<CommunityResponseDto.CommentResponse> comments = communityService.getCommentsByPostId(postId);
        return ResponseEntity.ok(comments);
    }

    /**
     * 댓글 작성
     */
    @PostMapping("/posts/{postId}/comments")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "댓글 작성", description = "게시글에 댓글을 작성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "작성 성공",
            content = @Content(schema = @Schema(implementation = CommunityResponseDto.CommentResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<CommunityResponseDto.CommentResponse> createComment(
            @Parameter(description = "게시글 ID", required = true) @PathVariable Long postId,
            @Parameter(description = "댓글 정보", required = true) @RequestBody CommunityRequestDto.CreateCommentRequest request) {
        log.info("댓글 작성: 게시글ID={}", postId);
        CommunityResponseDto.CommentResponse comment = communityService.createComment(postId, request);
        return ResponseEntity.ok(comment);
    }

    /**
     * 댓글 수정
     */
    @PutMapping("/comments/{commentId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "댓글 수정", description = "기존 댓글을 수정합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "수정 성공",
            content = @Content(schema = @Schema(implementation = CommunityResponseDto.CommentResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<CommunityResponseDto.CommentResponse> updateComment(
            @Parameter(description = "댓글 ID", required = true) @PathVariable Long commentId,
            @Parameter(description = "수정할 댓글 정보", required = true) @RequestBody CommunityRequestDto.UpdateCommentRequest request) {
        log.info("댓글 수정: 댓글ID={}", commentId);
        CommunityResponseDto.CommentResponse comment = communityService.updateComment(commentId, request);
        return ResponseEntity.ok(comment);
    }

    /**
     * 댓글 삭제
     */
    @DeleteMapping("/comments/{commentId}")
    @LogExecutionTime
    @RequireAuthentication
    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, String>> deleteComment(
            @Parameter(description = "댓글 ID", required = true) @PathVariable Long commentId) {
        log.info("댓글 삭제: 댓글ID={}", commentId);
        communityService.deleteComment(commentId);
        return ResponseEntity.ok(Map.of("message", "댓글이 삭제되었습니다."));
    }

    /**
     * 게시글 검색
     */
    @GetMapping("/search")
    @LogExecutionTime
    @Operation(summary = "게시글 검색", description = "키워드로 게시글을 검색합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "검색 성공",
            content = @Content(schema = @Schema(implementation = CommunityResponseDto.PostResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<CommunityResponseDto.PostResponse>> searchPosts(
            @Parameter(description = "검색 키워드", required = true) @RequestParam String keyword) {
        log.info("게시글 검색: 키워드={}", keyword);
        List<CommunityResponseDto.PostResponse> posts = communityService.searchPosts(keyword);
        return ResponseEntity.ok(posts);
    }

    /**
     * 인기 게시글 조회
     */
    @GetMapping("/popular")
    @LogExecutionTime
    @Operation(summary = "인기 게시글 조회", description = "인기 있는 게시글 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = CommunityResponseDto.PostResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<CommunityResponseDto.PostResponse>> getPopularPosts(
            @Parameter(description = "조회할 게시글 수", example = "10") @RequestParam(defaultValue = "10") Integer limit) {
        log.info("인기 게시글 조회: 제한={}", limit);
        List<CommunityResponseDto.PostResponse> posts = communityService.getPopularPosts(limit);
        return ResponseEntity.ok(posts);
    }

    /**
     * 최신 게시글 조회
     */
    @GetMapping("/latest")
    @LogExecutionTime
    @Operation(summary = "최신 게시글 조회", description = "최근 작성된 게시글 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = CommunityResponseDto.PostResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<CommunityResponseDto.PostResponse>> getLatestPosts(
            @Parameter(description = "조회할 게시글 수", example = "10") @RequestParam(defaultValue = "10") Integer limit) {
        log.info("최신 게시글 조회: 제한={}", limit);
        List<CommunityResponseDto.PostResponse> posts = communityService.getLatestPosts(limit);
        return ResponseEntity.ok(posts);
    }
} 