package com.carecode.domain.user.repository;

import com.carecode.domain.user.entity.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 북마크 리포지토리 인터페이스
 */
@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    
    /**
     * 사용자별 북마크 목록 조회
     */
    Page<Bookmark> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * 사용자별 특정 타입의 북마크 목록 조회
     */
    List<Bookmark> findByUserIdAndTargetTypeOrderByCreatedAtDesc(Long userId, String targetType);
    
    /**
     * 사용자가 특정 타겟을 북마크했는지 확인
     */
    Optional<Bookmark> findByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);
    
    /**
     * 특정 타겟의 북마크 수 조회
     */
    long countByTargetTypeAndTargetId(String targetType, Long targetId);
    
    /**
     * 사용자의 북마크 수 조회
     */
    long countByUserId(Long userId);
    
    /**
     * 게시글 북마크 목록 조회
     */
    @Query("SELECT b FROM Bookmark b WHERE b.targetType = 'POST' AND b.targetId = :postId")
    List<Bookmark> findByPostId(@Param("postId") Long postId);
    
    /**
     * 정책 북마크 목록 조회
     */
    @Query("SELECT b FROM Bookmark b WHERE b.targetType = 'POLICY' AND b.targetId = :policyId")
    List<Bookmark> findByPolicyId(@Param("policyId") Long policyId);
    
    /**
     * 시설 북마크 목록 조회
     */
    @Query("SELECT b FROM Bookmark b WHERE b.targetType = 'FACILITY' AND b.targetId = :facilityId")
    List<Bookmark> findByFacilityId(@Param("facilityId") Long facilityId);
    
    /**
     * 노트가 있는 북마크 목록 조회
     */
    @Query("SELECT b FROM Bookmark b WHERE b.user.id = :userId AND b.note IS NOT NULL AND b.note != ''")
    List<Bookmark> findByUserIdAndHasNote(@Param("userId") Long userId);
} 