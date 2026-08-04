package com.carecode.domain.community.service;

import com.carecode.core.exception.CommentAccessDeniedException;
import com.carecode.core.exception.PostAccessDeniedException;
import com.carecode.domain.community.dto.request.CommunityUpdateCommentRequest;
import com.carecode.domain.community.dto.request.CommunityUpdatePostRequest;
import com.carecode.domain.community.entity.Comment;
import com.carecode.domain.community.entity.Post;
import com.carecode.domain.community.mapper.CommunityMapper;
import com.carecode.domain.community.repository.BookmarkRepository;
import com.carecode.domain.community.repository.CommentRepository;
import com.carecode.domain.community.repository.PostLikeRepository;
import com.carecode.domain.community.repository.PostRepository;
import com.carecode.domain.community.repository.TagRepository;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.entity.UserRole;
import com.carecode.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 커뮤니티 게시글/댓글 소유권 검증에 대한 회귀 테스트.
 *
 * <p>과거에는 수정·삭제 시 작성자 확인이 전혀 없어서
 * 로그인한 사용자라면 누구나 남의 글과 댓글을 지울 수 있었다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CommunityService - 소유권 검증")
class CommunityServiceOwnershipTest {

    private static final String OWNER_EMAIL = "owner@example.com";
    private static final String OTHER_EMAIL = "other@example.com";
    private static final String ADMIN_EMAIL = "admin@example.com";

    @Mock private PostRepository postRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private TagRepository tagRepository;
    @Mock private UserRepository userRepository;
    @Mock private PostLikeRepository postLikeRepository;
    @Mock private BookmarkRepository bookmarkRepository;
    @Mock private CommunityMapper communityMapper;

    @InjectMocks private CommunityService communityService;

    private User owner;
    private User other;
    private User admin;
    private Post post;
    private Comment comment;

    @BeforeEach
    void setUp() {
        owner = user(1L, OWNER_EMAIL, UserRole.PARENT);
        other = user(2L, OTHER_EMAIL, UserRole.PARENT);
        admin = user(3L, ADMIN_EMAIL, UserRole.ADMIN);

        post = Post.builder().id(100L).title("제목").content("내용").author(owner).build();
        comment = Comment.builder().id(200L).content("댓글").author(owner).post(post).build();

        when(userRepository.findByEmailAndDeletedAtIsNull(OWNER_EMAIL)).thenReturn(Optional.of(owner));
        when(userRepository.findByEmailAndDeletedAtIsNull(OTHER_EMAIL)).thenReturn(Optional.of(other));
        when(userRepository.findByEmailAndDeletedAtIsNull(ADMIN_EMAIL)).thenReturn(Optional.of(admin));
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        when(commentRepository.findById(200L)).thenReturn(Optional.of(comment));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(commentRepository.findByPostIdAndParentCommentIsNull(any())).thenReturn(List.of());
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("타인의 게시글은 삭제할 수 없다")
    void cannotDeleteOthersPost() {
        authenticateAs(OTHER_EMAIL, "PARENT");

        assertThatThrownBy(() -> communityService.deletePost(100L))
                .isInstanceOf(PostAccessDeniedException.class);

        verify(postRepository, never()).delete(any(Post.class));
    }

    @Test
    @DisplayName("타인의 게시글은 수정할 수 없다")
    void cannotUpdateOthersPost() {
        authenticateAs(OTHER_EMAIL, "PARENT");
        CommunityUpdatePostRequest request = new CommunityUpdatePostRequest();

        assertThatThrownBy(() -> communityService.updatePost(100L, request))
                .isInstanceOf(PostAccessDeniedException.class);

        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("타인의 댓글은 삭제할 수 없다")
    void cannotDeleteOthersComment() {
        authenticateAs(OTHER_EMAIL, "PARENT");

        assertThatThrownBy(() -> communityService.deleteComment(200L))
                .isInstanceOf(CommentAccessDeniedException.class);

        verify(commentRepository, never()).delete(any(Comment.class));
    }

    @Test
    @DisplayName("타인의 댓글은 수정할 수 없다")
    void cannotUpdateOthersComment() {
        authenticateAs(OTHER_EMAIL, "PARENT");
        CommunityUpdateCommentRequest request = new CommunityUpdateCommentRequest();

        assertThatThrownBy(() -> communityService.updateComment(200L, request))
                .isInstanceOf(CommentAccessDeniedException.class);

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("작성자 본인은 자신의 게시글을 삭제할 수 있다")
    void ownerCanDeleteOwnPost() {
        authenticateAs(OWNER_EMAIL, "PARENT");

        assertThatCode(() -> communityService.deletePost(100L)).doesNotThrowAnyException();

        verify(postRepository).delete(post);
    }

    @Test
    @DisplayName("관리자는 타인의 게시글을 삭제할 수 있다")
    void adminCanDeleteAnyPost() {
        authenticateAs(ADMIN_EMAIL, "ADMIN");

        assertThatCode(() -> communityService.deletePost(100L)).doesNotThrowAnyException();

        verify(postRepository).delete(post);
    }

    private void authenticateAs(String email, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        email, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private static User user(Long id, String email, UserRole role) {
        return User.builder()
                .id(id)
                .userId("u-" + id)
                .email(email)
                .name("user" + id)
                .role(role)
                .build();
    }
}
