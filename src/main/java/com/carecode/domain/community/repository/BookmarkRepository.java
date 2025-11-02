package com.carecode.domain.community.repository;

import com.carecode.domain.community.entity.Bookmark;
import com.carecode.domain.community.entity.Post;
import com.carecode.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 북마크 Repository
 */
@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    /**
     * 특정 사용자가 특정 게시글을 북마크했는지 확인
     */
    boolean existsByPostAndUser(Post post, User user);

    /**
     * 특정 사용자가 특정 게시글에 한 북마크 조회
     */
    Optional<Bookmark> findByPostAndUser(Post post, User user);

    /**
     * 특정 게시글의 북마크 개수
     */
    long countByPost(Post post);

    /**
     * 특정 사용자가 북마크한 게시글 목록
     */
    List<Bookmark> findByUser(User user);

    /**
     * 특정 게시글의 모든 북마크 삭제
     */
    void deleteByPost(Post post);

    /**
     * 특정 사용자와 게시글의 북마크 삭제
     */
    void deleteByPostAndUser(Post post, User user);

    /**
     * 특정 게시글 목록에 대해 사용자가 북마크했는지 확인
     */
    @Query("SELECT b.post.id FROM Bookmark b WHERE b.user = :user AND b.post.id IN :postIds")
    List<Long> findBookmarkedPostIdsByUserAndPostIds(@Param("user") User user, @Param("postIds") List<Long> postIds);
}
