package com.carecode.domain.user.repository;

import com.carecode.domain.user.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 좋아요 리포지토리 인터페이스
 */
@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    
    /**
     * 사용자별 좋아요 목록 조회
     */
    List<Like> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * 특정 타겟에 대한 좋아요 목록 조회
     */
    List<Like> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, Long targetId);
    
    /**
     * 사용자가 특정 타겟에 좋아요를 눌렀는지 확인
     */
    Optional<Like> findByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);
    
    /**
     * 특정 타겟의 좋아요 수 조회
     */
    long countByTargetTypeAndTargetId(String targetType, Long targetId);
    
    /**
     * 사용자의 좋아요 수 조회
     */
    long countByUserId(Long userId);
    
    /**
     * 게시글 좋아요 목록 조회
     */
    @Query("SELECT l FROM Like l WHERE l.targetType = 'POST' AND l.targetId = :postId")
    List<Like> findByPostId(@Param("postId") Long postId);
    
    /**
     * 댓글 좋아요 목록 조회
     */
    @Query("SELECT l FROM Like l WHERE l.targetType = 'COMMENT' AND l.targetId = :commentId")
    List<Like> findByCommentId(@Param("commentId") Long commentId);
    
    /**
     * 리뷰 좋아요 목록 조회
     */
    @Query("SELECT l FROM Like l WHERE l.targetType = 'REVIEW' AND l.targetId = :reviewId")
    List<Like> findByReviewId(@Param("reviewId") Long reviewId);
} 