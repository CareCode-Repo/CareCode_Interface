package com.carecode.domain.community.repository;

import com.carecode.domain.community.entity.Post;
import com.carecode.domain.community.entity.PostLike;
import com.carecode.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 게시글 좋아요 Repository
 */
@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    /**
     * 특정 사용자가 특정 게시글에 좋아요를 눌렀는지 확인
     */
    boolean existsByPostAndUser(Post post, User user);

    /**
     * 특정 사용자가 특정 게시글에 누른 좋아요 조회
     */
    Optional<PostLike> findByPostAndUser(Post post, User user);

    /**
     * 특정 게시글의 좋아요 개수
     */
    long countByPost(Post post);

    /**
     * 특정 사용자가 좋아요한 게시글 목록
     */
    List<PostLike> findByUser(User user);

    /**
     * 특정 게시글의 모든 좋아요 삭제
     */
    void deleteByPost(Post post);

    /**
     * 특정 사용자와 게시글의 좋아요 삭제
     */
    void deleteByPostAndUser(Post post, User user);

    /**
     * 특정 게시글 목록에 대해 사용자가 좋아요를 눌렀는지 확인
     */
    @Query("SELECT pl.post.id FROM PostLike pl WHERE pl.user = :user AND pl.post.id IN :postIds")
    List<Long> findLikedPostIdsByUserAndPostIds(@Param("user") User user, @Param("postIds") List<Long> postIds);
}
