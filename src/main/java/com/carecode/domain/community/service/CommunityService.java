package com.carecode.domain.community.service;

import com.carecode.domain.community.entity.Post;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 커뮤니티 서비스 클래스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityService {
    
    /**
     * 게시글 목록 조회
     */
    public List<Post> getPosts(String category) {
        log.info("게시글 목록 조회 - 카테고리: {}", category);
        // 실제로는 데이터베이스에서 조회
        return List.of();
    }
    
    /**
     * 게시글 상세 조회
     */
    public Post getPost(Long postId) {
        log.info("게시글 상세 조회 - 게시글 ID: {}", postId);
        // 실제로는 데이터베이스에서 조회
        return null;
    }
    
    /**
     * 게시글 작성
     */
    public Post createPost(Post post) {
        log.info("게시글 작성 - 제목: {}", post.getTitle());
        // 실제로는 데이터베이스에 저장
        return post;
    }
} 