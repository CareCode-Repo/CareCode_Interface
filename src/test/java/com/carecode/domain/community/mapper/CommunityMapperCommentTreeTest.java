package com.carecode.domain.community.mapper;

import com.carecode.domain.community.dto.response.CommunityCommentResponse;
import com.carecode.domain.community.entity.Comment;
import com.carecode.domain.community.repository.CommentRepository;
import com.carecode.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 댓글 트리 조립에 대한 테스트.
 *
 * <p>예전 매퍼는 최상위 댓글만 조회한 뒤 {@code comment.getReplies()} 를 재귀로 따라갔다.
 * 지연 로딩 컬렉션이라 노드 하나마다 쿼리가 나갔고, 댓글이 많은 글일수록 급격히 느려졌다.
 * 지금은 평평한 목록을 한 번에 읽어 메모리에서 트리를 만든다. 그 조립이 정확한지 고정한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommunityMapper - 댓글 트리 조립")
class CommunityMapperCommentTreeTest {

    @Mock private CommentRepository commentRepository;

    @InjectMocks private CommunityMapper mapper;

    private static Comment comment(long id, Comment parent) {
        return Comment.builder()
                .id(id)
                .content("내용 " + id)
                .authorName("작성자")
                .author(User.builder().id(100L).userId("user_a").build())
                .likeCount(0)
                .isActive(true)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0).plusMinutes(id))
                .parentComment(parent)
                .build();
    }

    @Test
    @DisplayName("부모-자식 관계를 그대로 복원한다")
    void buildsTree() {
        Comment root1 = comment(1L, null);
        Comment root2 = comment(2L, null);
        Comment reply1 = comment(3L, root1);
        Comment reply2 = comment(4L, root1);
        Comment nested = comment(5L, reply1);

        List<CommunityCommentResponse> tree =
                mapper.toCommentTree(List.of(root1, root2, reply1, reply2, nested));

        assertThat(tree).hasSize(2);
        assertThat(tree).extracting(CommunityCommentResponse::getCommentId).containsExactly(1L, 2L);

        CommunityCommentResponse first = tree.get(0);
        assertThat(first.getReplies()).extracting(CommunityCommentResponse::getCommentId)
                .containsExactly(3L, 4L);
        assertThat(first.getReplies().get(0).getReplies())
                .extracting(CommunityCommentResponse::getCommentId)
                .containsExactly(5L);
        assertThat(tree.get(1).getReplies()).isEmpty();
    }

    @Test
    @DisplayName("부모가 목록에 없는 답글은 버리지 않고 최상위로 올린다")
    void keepsOrphanReplies() {
        // 부모가 신고로 숨김 처리되면 조회 결과에서 빠진다. 그때 답글까지 사라지면
        // 사용자 입장에서는 자기 댓글이 이유 없이 없어진 것으로 보인다.
        Comment hiddenParent = comment(10L, null);
        Comment orphan = comment(11L, hiddenParent);

        List<CommunityCommentResponse> tree = mapper.toCommentTree(List.of(orphan));

        assertThat(tree).extracting(CommunityCommentResponse::getCommentId).containsExactly(11L);
        assertThat(tree.get(0).getParentCommentId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("빈 목록은 빈 트리를 준다")
    void handlesEmpty() {
        assertThat(mapper.toCommentTree(List.of())).isEmpty();
        assertThat(mapper.toCommentTree(null)).isEmpty();
    }

    @Test
    @DisplayName("단건 변환은 답글을 채우지 않는다")
    void singleMappingDoesNotTouchReplies() {
        // 여기서 답글을 재귀로 채우면 다시 N+1 이 된다. 트리가 필요하면 toCommentTree 를 쓴다.
        Comment root = comment(1L, null);
        root.setReplies(List.of(comment(2L, root)));

        CommunityCommentResponse response = mapper.toCommentResponse(root);

        assertThat(response.getReplies()).isEmpty();
    }
}
