package com.carecode.domain.community.app;

import com.carecode.domain.community.dto.CommunityRequest;
import com.carecode.domain.community.dto.CommunityResponse;
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
    public CommunityResponse.PageResponse<CommunityResponse.PostResponse> getAllPosts(int page, int size) {
        return communityService.getAllPosts(page, size);
    }

    @Transactional(readOnly = true)
    public CommunityResponse.PostDetailResponse getPostDetailById(Long postId) {
        return communityService.getPostById(postId);
    }

    @Transactional
    public CommunityResponse.PostResponse createPost(CommunityRequest.CreatePost request) {
        return communityService.createPost(request);
    }

    @Transactional
    public CommunityResponse.PostResponse updatePost(Long postId, CommunityRequest.UpdatePost request) {
        return communityService.updatePost(postId, request);
    }

    @Transactional
    public void deletePost(Long postId) {
        communityService.deletePost(postId);
    }

    // ===== Comments =====
    @Transactional(readOnly = true)
    public List<CommunityResponse.CommentResponse> getCommentsByPostId(Long postId) {
        return communityService.getCommentsByPostId(postId);
    }

    @Transactional
    public CommunityResponse.CommentResponse createComment(Long postId, CommunityRequest.CreateComment request) {
        return communityService.createComment(postId, request);
    }

    @Transactional
    public CommunityResponse.CommentResponse updateComment(Long commentId, CommunityRequest.UpdateComment request) {
        return communityService.updateComment(commentId, request);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        communityService.deleteComment(commentId);
    }

    // ===== Search & Lists =====
    @Transactional(readOnly = true)
    public CommunityResponse.PageResponse<CommunityResponse.PostResponse> searchPosts(String keyword, int page, int size) {
        return communityService.searchPosts(keyword, page, size);
    }

    @Transactional(readOnly = true)
    public CommunityResponse.PageResponse<CommunityResponse.PostResponse> getPopularPosts(int page, int size) {
        return communityService.getPopularPosts(page, size);
    }

    @Transactional(readOnly = true)
    public CommunityResponse.PageResponse<CommunityResponse.PostResponse> getLatestPosts(int page, int size) {
        return communityService.getLatestPosts(page, size);
    }

    // ===== Tags =====
    @Transactional(readOnly = true)
    public List<CommunityResponse.TagResponse> getAllTags() {
        return communityService.getAllTags();
    }
}


