package com.carecode.domain.community.app;

import com.carecode.domain.community.dto.request.CommunityRequest;
import com.carecode.domain.community.dto.request.CommunityCreatePostRequest;
import com.carecode.domain.community.dto.request.CommunityUpdatePostRequest;
import com.carecode.domain.community.dto.request.CommunityCreateCommentRequest;
import com.carecode.domain.community.dto.request.CommunityUpdateCommentRequest;
import com.carecode.domain.community.dto.response.CommunityResponse;
import com.carecode.domain.community.dto.response.CommunityPostResponse;
import com.carecode.domain.community.dto.response.CommunityPostDetailResponse;
import com.carecode.domain.community.dto.response.CommunityCommentResponse;
import com.carecode.domain.community.dto.response.CommunityTagResponse;
import com.carecode.domain.community.dto.response.CommunityPageResponse;
import com.carecode.domain.community.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunityFacade {

    private final CommunityService communityService;

    // ===== Posts =====
    @Transactional(readOnly = true)
    public CommunityPageResponse<CommunityPostResponse> getAllPosts(int page, int size, String sortBy, String sortDirection) {
        return communityService.getAllPosts(page, size, sortBy, sortDirection);
    }

    @Transactional(readOnly = true)
    public CommunityPostDetailResponse getPostDetailById(Long postId) {
        return communityService.getPostById(postId);
    }

    @Transactional
    public CommunityPostResponse createPost(CommunityCreatePostRequest request) {
        return communityService.createPost(request);
    }

    @Transactional
    public CommunityPostResponse updatePost(Long postId, CommunityUpdatePostRequest request) {
        return communityService.updatePost(postId, request);
    }

    @Transactional
    public void deletePost(Long postId) {
        communityService.deletePost(postId);
    }

    // ===== Comments =====
    @Transactional(readOnly = true)
    public List<CommunityCommentResponse> getCommentsByPostId(Long postId) {
        return communityService.getCommentsByPostId(postId);
    }

    @Transactional
    public CommunityCommentResponse createComment(Long postId, CommunityCreateCommentRequest request) {
        return communityService.createComment(postId, request);
    }

    @Transactional
    public CommunityCommentResponse updateComment(Long commentId, CommunityUpdateCommentRequest request) {
        return communityService.updateComment(commentId, request);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        communityService.deleteComment(commentId);
    }

    // ===== Search & Lists =====
    @Transactional(readOnly = true)
    public CommunityPageResponse<CommunityPostResponse> searchPosts(String keyword, int page, int size) {
        return communityService.searchPosts(keyword, page, size);
    }

    @Transactional(readOnly = true)
    public CommunityPageResponse<CommunityPostResponse> getPopularPosts(int page, int size) {
        return communityService.getPopularPosts(page, size);
    }

    @Transactional(readOnly = true)
    public CommunityPageResponse<CommunityPostResponse> getLatestPosts(int page, int size) {
        return communityService.getLatestPosts(page, size);
    }

    // ===== Tags =====
    @Transactional(readOnly = true)
    public List<CommunityTagResponse> getAllTags() {
        return communityService.getAllTags();
    }

    // ===== 좋아요 및 북마크 =====
    @Transactional
    public boolean toggleLike(Long postId, Long userId) {
        return communityService.toggleLike(postId, userId);
    }

    @Transactional
    public boolean toggleBookmark(Long postId, Long userId) {
        return communityService.toggleBookmark(postId, userId);
    }

    @Transactional(readOnly = true)
    public List<CommunityPostResponse> getLikedPosts(Long userId) {
        return communityService.getLikedPosts(userId);
    }

    @Transactional(readOnly = true)
    public List<CommunityPostResponse> getBookmarkedPosts(Long userId) {
        return communityService.getBookmarkedPosts(userId);
    }

    @Transactional(readOnly = true)
    public long getLikeCount(Long postId) {
        return communityService.getLikeCount(postId);
    }

    @Transactional(readOnly = true)
    public long getBookmarkCount(Long postId) {
        return communityService.getBookmarkCount(postId);
    }
}


