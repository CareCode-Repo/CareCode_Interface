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
    

    // 사용자 ID로 사용자 조회 (삭제되지 않은 사용자만)
    @Query("SELECT u FROM User u WHERE u.userId = :userId AND u.deletedAt IS NULL")
    Optional<User> findByUserIdAndDeletedAtIsNull(@Param("userId") String userId);

    // 이메일로 사용자 조회 (삭제되지 않은 사용자만)
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findByEmailAndDeletedAtIsNull(@Param("email") String email);

    // 이름으로 사용자 조회 (삭제되지 않은 사용자만)
    @Query("SELECT u FROM User u WHERE u.name = :name AND u.deletedAt IS NULL")
    Optional<User> findByNameAndDeletedAtIsNull(@Param("name") String name);

    // 사용자 ID로 사용자 조회 (삭제된 사용자 포함)
    Optional<User> findByUserId(String userId);

    // 이메일로 사용자 조회 (삭제된 사용자 포함)
    Optional<User> findByEmail(String email);

    // 이름으로 사용자 조회 (삭제된 사용자 포함)
    Optional<User> findByName(String name);

    // 활성화된 사용자 목록 조회 (삭제되지 않은 사용자만)
    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.deletedAt IS NULL")
    List<User> findActiveUsersNotDeleted();

    // 사용자 역할별 조회 (삭제되지 않은 사용자만)
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.deletedAt IS NULL")
    List<User> findByRoleAndDeletedAtIsNull(@Param("role") String role);

    // 지역별 사용자 조회 (삭제되지 않은 사용자만)
    @Query("SELECT u FROM User u WHERE u.address LIKE %:region% AND u.deletedAt IS NULL")
    List<User> findByAddressContainingAndDeletedAtIsNull(@Param("region") String region);

    // 이메일 인증된 사용자 목록 조회 (삭제되지 않은 사용자만)
    @Query("SELECT u FROM User u WHERE u.emailVerified = true AND u.deletedAt IS NULL")
    List<User> findEmailVerifiedUsersNotDeleted();

    // 활성화된 사용자 목록 조회 (삭제된 사용자 포함)
    List<User> findByIsActiveTrue();

    // 사용자 역할별 조회 (삭제된 사용자 포함)
    List<User> findByRole(String role);

    // 지역별 사용자 조회 (삭제된 사용자 포함)
    List<User> findByAddressContaining(String region);

    // 이메일 인증된 사용자 목록 조회 (삭제된 사용자 포함)
    List<User> findByEmailVerifiedTrue();

    // 최근 로그인한 사용자 목록 조회 (삭제되지 않은 사용자만)
    @Query("SELECT u FROM User u WHERE u.lastLoginAt IS NOT NULL AND u.deletedAt IS NULL ORDER BY u.lastLoginAt DESC")
    List<User> findRecentlyActiveUsersNotDeleted();

    // 특정 날짜 이후 업데이트된 사용자 목록 조회 (삭제되지 않은 사용자만)
    @Query("SELECT u FROM User u WHERE u.updatedAt > :dateTime AND u.deletedAt IS NULL")
    List<User> findByUpdatedAtAfterAndDeletedAtIsNull(@Param("dateTime") LocalDateTime dateTime);

    // 이름 또는 이메일로 사용자 검색 (삭제되지 않은 사용자만)
    @Query("SELECT u FROM User u WHERE (u.name LIKE %:name% OR u.email LIKE %:email%) AND u.deletedAt IS NULL")
    List<User> findByNameContainingOrEmailContainingAndDeletedAtIsNull(@Param("name") String name, @Param("email") String email);

    // 이름으로 사용자 검색 (삭제되지 않은 사용자만)
    @Query("SELECT u FROM User u WHERE u.name LIKE %:name% AND u.deletedAt IS NULL")
    List<User> findByNameContainingAndDeletedAtIsNull(@Param("name") String name);

    // 이메일로 사용자 검색 (삭제되지 않은 사용자만)
    @Query("SELECT u FROM User u WHERE u.email LIKE %:email% AND u.deletedAt IS NULL")
    List<User> findByEmailContainingAndDeletedAtIsNull(@Param("email") String email);

    // 키워드로 사용자 검색 (삭제되지 않은 사용자만)
    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.deletedAt IS NULL AND " +
           "(u.name LIKE %:keyword% OR u.email LIKE %:keyword% OR u.address LIKE %:keyword%)")
    List<User> searchByKeywordNotDeleted(@Param("keyword") String keyword);

    // 최근 로그인한 사용자 목록 조회 (삭제된 사용자 포함)
    @Query("SELECT u FROM User u WHERE u.lastLoginAt IS NOT NULL ORDER BY u.lastLoginAt DESC")
    List<User> findRecentlyActiveUsers();

    // 특정 날짜 이후 업데이트된 사용자 목록 조회 (삭제된 사용자 포함)
    List<User> findByUpdatedAtAfter(LocalDateTime dateTime);

    // 이름 또는 이메일로 사용자 검색 (삭제된 사용자 포함)
    List<User> findByNameContainingOrEmailContaining(String name, String email);

    // 키워드로 사용자 검색 (삭제된 사용자 포함)
    @Query("SELECT u FROM User u WHERE u.isActive = true AND " +
           "(u.name LIKE %:keyword% OR u.email LIKE %:keyword% OR u.address LIKE %:keyword%)")
    List<User> searchByKeyword(@Param("keyword") String keyword);

    // 이메일 존재 여부 확인 (삭제되지 않은 사용자만)
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    boolean existsByEmailAndDeletedAtIsNull(@Param("email") String email);

    // 사용자 ID 존재 여부 확인 (삭제되지 않은 사용자만)
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.userId = :userId AND u.deletedAt IS NULL")
    boolean existsByUserIdAndDeletedAtIsNull(@Param("userId") String userId);

    // 활성 사용자 수 조회 (삭제되지 않은 사용자만)
    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true AND u.deletedAt IS NULL")
    long countActiveUsersNotDeleted();

    // 이메일 인증된 사용자 수 조회 (삭제되지 않은 사용자만)
    @Query("SELECT COUNT(u) FROM User u WHERE u.emailVerified = true AND u.deletedAt IS NULL")
    long countEmailVerifiedUsersNotDeleted();

    // OAuth2 제공자와 프로바이더 ID로 사용자 조회 (삭제되지 않은 사용자만)
    @Query("SELECT u FROM User u WHERE u.provider = :provider AND u.providerId = :providerId AND u.deletedAt IS NULL")
    Optional<User> findByProviderAndProviderIdAndDeletedAtIsNull(@Param("provider") String provider, @Param("providerId") String providerId);

    // OAuth2 제공자로 사용자 조회 (삭제되지 않은 사용자만)
    @Query("SELECT u FROM User u WHERE u.provider = :provider AND u.deletedAt IS NULL")
    List<User> findByProviderAndDeletedAtIsNull(@Param("provider") String provider);

    // 이메일 존재 여부 확인 (삭제된 사용자 포함)
    boolean existsByEmail(String email);

    // 사용자 ID 존재 여부 확인 (삭제된 사용자 포함)
    boolean existsByUserId(String userId);

    // 활성 사용자 수 조회 (삭제된 사용자 포함)
    long countByIsActiveTrue();

    // 이메일 인증된 사용자 수 조회 (삭제된 사용자 포함)
    long countByEmailVerifiedTrue();

    // OAuth2 제공자와 프로바이더 ID로 사용자 조회 (삭제된 사용자 포함)
    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    // OAuth2 제공자로 사용자 조회 (삭제된 사용자 포함)
    List<User> findByProvider(String provider);
} 