package com.carecode.domain.community.repository;

import com.carecode.domain.community.entity.Post;
import com.carecode.domain.community.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 커뮤니티 게시글 리포지토리.
 *
 * <p>조회 메서드는 모두 {@code isActive = true} 를 건다. 신고 누적으로 자동 숨김된 글
 * (ModerationService)이 목록·상세에 다시 나타나면 숨김 기능이 사실상 없는 것과 같다.
 *
 * <p>{@code @EntityGraph(author)} 는 N+1 방지용이다. 응답 DTO 가 작성자 ID 를 쓰는데,
 * 필드 접근 엔티티의 프록시는 {@code getId()} 호출만으로도 초기화되어 게시글 수만큼
 * 사용자 조회가 나갔다. author 는 ToOne 이라 페이징과 함께 fetch 해도 안전하다.
 * (태그는 컬렉션이라 fetch join 대신 Post 쪽 {@code @BatchSize} 로 처리한다.)
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    /** 게시글 목록 (기본 정렬은 호출부의 Pageable 을 따른다). */
    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT p FROM Post p WHERE p.isActive = true")
    Page<Post> findAllActive(Pageable pageable);

    /** 상세 조회. 숨김 처리된 글은 ID 를 알아도 열리지 않아야 한다. */
    @EntityGraph(attributePaths = {"author", "tags"})
    @Query("SELECT p FROM Post p WHERE p.id = :postId AND p.isActive = true")
    Optional<Post> findActiveById(@Param("postId") Long postId);

    // 제목 또는 내용으로 검색
    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT p FROM Post p WHERE p.isActive = true AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%)")
    Page<Post> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 인기 게시글 조회 (좋아요 순) - 페이징
    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT p FROM Post p WHERE p.isActive = true ORDER BY p.likeCount DESC, p.createdAt DESC")
    Page<Post> findPopularPosts(Pageable pageable);

    // 최신 게시글 조회 - 페이징
    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT p FROM Post p WHERE p.isActive = true ORDER BY p.createdAt DESC")
    Page<Post> findLatestPosts(Pageable pageable);

    // 태그별 게시글 목록 조회
    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT p FROM Post p JOIN p.tags t WHERE t = :tag AND p.isActive = true")
    List<Post> findByTagsContaining(@Param("tag") Tag tag);

    long countByAuthorId(Long authorId);

    /** 조회수를 DB 에서 원자적으로 증가시킨다 (lost update 방지). 숨김 글은 세지 않는다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Post p SET p.viewCount = COALESCE(p.viewCount, 0) + 1 WHERE p.id = :postId AND p.isActive = true")
    int incrementViewCount(@Param("postId") Long postId);
}
