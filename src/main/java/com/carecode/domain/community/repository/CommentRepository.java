package com.carecode.domain.community.repository;

import com.carecode.domain.community.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 댓글 레포지토리
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    /**
     * 게시글 ID로 댓글 목록 조회 (부모 댓글만)
     */
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.parentComment IS NULL AND c.isActive = true ORDER BY c.createdAt ASC")
    List<Comment> findByPostIdAndParentCommentIsNull(@Param("postId") Long postId);
    
    /**
     * 게시글의 댓글 수 조회
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId AND c.isActive = true")
    long countByPostId(@Param("postId") Long postId);
} 