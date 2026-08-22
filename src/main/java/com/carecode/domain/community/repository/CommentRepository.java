package com.carecode.domain.community.repository;

import com.carecode.domain.community.entity.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/** 댓글 레포지토리 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 게시글 ID로 댓글 목록 조회 (부모 댓글만)
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.parentComment IS NULL AND c.isActive = true ORDER BY c.createdAt ASC")
    List<Comment> findByPostIdAndParentCommentIsNull(@Param("postId") Long postId);

    /**
     * 게시글의 활성 댓글 전체를 답글까지 한 번에 가져온다.
     *
     * <p>기존에는 최상위 댓글만 조회한 뒤 매퍼가 {@code comment.getReplies()} 를 재귀로 훑어서,
     * 댓글 트리의 노드 수만큼 쿼리가 나갔다. 깊이가 깊은 글일수록 급격히 나빠진다.
     * 평평하게 한 번에 읽고 트리는 메모리에서 조립한다(CommunityMapper 참고).
     *
     * <p>{@code parentComment} 를 함께 fetch 하는 이유는 응답에 부모 ID 가 들어가는데,
     * 프록시의 {@code getId()} 호출이 초기화를 유발해 다시 N+1 이 되기 때문이다.
     */
    @EntityGraph(attributePaths = {"author", "parentComment"})
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.isActive = true ORDER BY c.createdAt ASC")
    List<Comment> findActiveTreeByPostId(@Param("postId") Long postId);

    // 게시글의 댓글 수 조회
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId AND c.isActive = true")
    long countByPostId(@Param("postId") Long postId);
}
