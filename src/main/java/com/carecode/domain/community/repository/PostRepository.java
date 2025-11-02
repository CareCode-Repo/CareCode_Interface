package com.carecode.domain.community.repository;

import com.carecode.domain.community.entity.Post;
import com.carecode.domain.community.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 커뮤니티 게시글 리포지토리 인터페이스
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * 제목 또는 내용으로 검색
     */
    @Query("SELECT p FROM Post p WHERE p.isActive = true AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%)")
    Page<Post> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 인기 게시글 조회 (좋아요 순) - 페이징
     */
    @Query("SELECT p FROM Post p WHERE p.isActive = true ORDER BY p.likeCount DESC, p.createdAt DESC")
    Page<Post> findPopularPosts(Pageable pageable);

    /**
     * 최신 게시글 조회 - 페이징
     */
    @Query("SELECT p FROM Post p WHERE p.isActive = true ORDER BY p.createdAt DESC")
    Page<Post> findLatestPosts(Pageable pageable);
    

    /**
     * 태그별 게시글 목록 조회
     */
    @Query("SELECT p FROM Post p JOIN p.tags t WHERE t = :tag AND p.isActive = true")
    List<Post> findByTagsContaining(@Param("tag") Tag tag);
} 