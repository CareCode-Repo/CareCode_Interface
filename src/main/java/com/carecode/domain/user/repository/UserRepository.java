package com.carecode.domain.user.repository;

import com.carecode.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 리포지토리 인터페이스
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 사용자 ID로 사용자 조회
     */
    Optional<User> findByUserId(String userId);
    
    /**
     * 이메일로 사용자 조회
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 활성화된 사용자 목록 조회
     */
    List<User> findByIsActiveTrue();
    
    /**
     * 사용자 역할별 조회
     */
    List<User> findByRole(String role);
    
    /**
     * 지역별 사용자 조회
     */
    List<User> findByAddressContaining(String region);
    
    /**
     * 이메일 인증된 사용자 목록 조회
     */
    List<User> findByEmailVerifiedTrue();
    
    /**
     * 최근 로그인한 사용자 목록 조회
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt IS NOT NULL ORDER BY u.lastLoginAt DESC")
    List<User> findRecentlyActiveUsers();
    
    /**
     * 특정 날짜 이후 업데이트된 사용자 목록 조회
     */
    List<User> findByUpdatedAtAfter(LocalDateTime dateTime);
    
    /**
     * 이름 또는 이메일로 사용자 검색
     */
    List<User> findByNameContainingOrEmailContaining(String name, String email);
    
    /**
     * 키워드로 사용자 검색
     */
    @Query("SELECT u FROM User u WHERE u.isActive = true AND " +
           "(u.name LIKE %:keyword% OR u.email LIKE %:keyword% OR u.address LIKE %:keyword%)")
    List<User> searchByKeyword(@Param("keyword") String keyword);
    
    /**
     * 이메일 존재 여부 확인
     */
    boolean existsByEmail(String email);
    
    /**
     * 사용자 ID 존재 여부 확인
     */
    boolean existsByUserId(String userId);
    
    /**
     * 활성화된 사용자 수 조회
     */
    long countByIsActiveTrue();
    
    /**
     * 이메일 인증된 사용자 수 조회
     */
    long countByEmailVerifiedTrue();
} 