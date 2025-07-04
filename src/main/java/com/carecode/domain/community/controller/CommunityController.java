package com.carecode.domain.community.controller;

import com.carecode.core.annotation.LogExecutionTime;
import com.carecode.core.annotation.RequireAuthentication;
import com.carecode.core.controller.BaseController;
import com.carecode.core.exception.CareServiceException;
import com.carecode.domain.community.dto.CommunityRequestDto;
import com.carecode.domain.community.dto.CommunityResponseDto;
import com.carecode.domain.community.service.CommunityService;
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
public class CommunityController extends BaseController {

    private final CommunityService communityService;

    /**
     * 게시글 목록 조회
     */
    @GetMapping("/posts")
    @LogExecutionTime
    public ResponseEntity<List<CommunityResponseDto.PostResponse>> getAllPosts() {
        log.info("게시글 목록 조회");
        
        try {
            List<CommunityResponseDto.PostResponse> posts = communityService.getAllPosts();
            return ResponseEntity.ok(posts);
        } catch (CareServiceException e) {
            log.error("게시글 목록 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 게시글 상세 조회
     */
    @GetMapping("/posts/{postId}")
    @LogExecutionTime
    public ResponseEntity<CommunityResponseDto.PostDetailResponse> getPost(@PathVariable Long postId) {
        log.info("게시글 상세 조회: 게시글ID={}", postId);
        
        try {
            CommunityResponseDto.PostDetailResponse post = communityService.getPostById(postId);
            return ResponseEntity.ok(post);
        } catch (CareServiceException e) {
            log.error("게시글 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 게시글 작성
     */
    @PostMapping("/posts")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<CommunityResponseDto.PostResponse> createPost(@RequestBody CommunityRequestDto.CreatePostRequest request) {
        log.info("게시글 작성: 제목={}", request.getTitle());
        
        try {
            CommunityResponseDto.PostResponse post = communityService.createPost(request);
            return ResponseEntity.ok(post);
        } catch (CareServiceException e) {
            log.error("게시글 작성 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 게시글 수정
     */
    @PutMapping("/posts/{postId}")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<CommunityResponseDto.PostResponse> updatePost(
            @PathVariable Long postId,
            @RequestBody CommunityRequestDto.UpdatePostRequest request) {
        log.info("게시글 수정: 게시글ID={}", postId);
        
        try {
            CommunityResponseDto.PostResponse post = communityService.updatePost(postId, request);
            return ResponseEntity.ok(post);
        } catch (CareServiceException e) {
            log.error("게시글 수정 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 게시글 삭제
     */
    @DeleteMapping("/posts/{postId}")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<Map<String, String>> deletePost(@PathVariable Long postId) {
        log.info("게시글 삭제: 게시글ID={}", postId);
        
        try {
            communityService.deletePost(postId);
            return ResponseEntity.ok(Map.of("message", SUCCESS_MESSAGE));
        } catch (CareServiceException e) {
            log.error("게시글 삭제 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 댓글 목록 조회
     */
    @GetMapping("/posts/{postId}/comments")
    @LogExecutionTime
    public ResponseEntity<List<CommunityResponseDto.CommentResponse>> getComments(@PathVariable Long postId) {
        log.info("댓글 목록 조회: 게시글ID={}", postId);
        
        try {
            List<CommunityResponseDto.CommentResponse> comments = communityService.getCommentsByPostId(postId);
            return ResponseEntity.ok(comments);
        } catch (CareServiceException e) {
            log.error("댓글 목록 조회 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 댓글 작성
     */
    @PostMapping("/posts/{postId}/comments")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<CommunityResponseDto.CommentResponse> createComment(
            @PathVariable Long postId,
            @RequestBody CommunityRequestDto.CreateCommentRequest request) {
        log.info("댓글 작성: 게시글ID={}", postId);
        
        try {
            CommunityResponseDto.CommentResponse comment = communityService.createComment(postId, request);
            return ResponseEntity.ok(comment);
        } catch (CareServiceException e) {
            log.error("댓글 작성 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 댓글 수정
     */
    @PutMapping("/comments/{commentId}")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<CommunityResponseDto.CommentResponse> updateComment(
            @PathVariable Long commentId,
            @RequestBody CommunityRequestDto.UpdateCommentRequest request) {
        log.info("댓글 수정: 댓글ID={}", commentId);
        
        try {
            CommunityResponseDto.CommentResponse comment = communityService.updateComment(commentId, request);
            return ResponseEntity.ok(comment);
        } catch (CareServiceException e) {
            log.error("댓글 수정 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 댓글 삭제
     */
    @DeleteMapping("/comments/{commentId}")
    @LogExecutionTime
    @RequireAuthentication
    public ResponseEntity<Map<String, String>> deleteComment(@PathVariable Long commentId) {
        log.info("댓글 삭제: 댓글ID={}", commentId);
        
        try {
            communityService.deleteComment(commentId);
            return ResponseEntity.ok(Map.of("message", SUCCESS_MESSAGE));
        } catch (CareServiceException e) {
            log.error("댓글 삭제 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 게시글 검색
     */
    @GetMapping("/search")
    @LogExecutionTime
    public ResponseEntity<List<CommunityResponseDto.PostResponse>> searchPosts(@RequestParam String keyword) {
        log.info("게시글 검색: 키워드={}", keyword);
        
        try {
            List<CommunityResponseDto.PostResponse> posts = communityService.searchPosts(keyword);
            return ResponseEntity.ok(posts);
        } catch (CareServiceException e) {
            log.error("게시글 검색 오류: {}", e.getMessage());
            throw e;
        }
    }
} 