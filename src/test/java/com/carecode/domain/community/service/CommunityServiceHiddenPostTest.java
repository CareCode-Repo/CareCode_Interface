package com.carecode.domain.community.service;

import com.carecode.core.exception.ResourceNotFoundException;
import com.carecode.domain.community.entity.Post;
import com.carecode.domain.community.mapper.CommunityMapper;
import com.carecode.domain.community.repository.BookmarkRepository;
import com.carecode.domain.community.repository.CommentRepository;
import com.carecode.domain.community.repository.PostLikeRepository;
import com.carecode.domain.community.repository.PostRepository;
import com.carecode.domain.community.repository.TagRepository;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 신고 자동 숨김이 실제로 감추는지에 대한 회귀 테스트.
 *
 * <p>ModerationService 는 신고가 임계치를 넘으면 {@code post.isActive = false} 로 글을 감춘다.
 * 그런데 기본 목록 조회는 {@code findAll} 을 써서 필터가 없었고, 상세 조회도 {@code findById} 라
 * 링크만 알면 그대로 열렸다. 즉 숨김 기능이 사실상 동작하지 않았다.
 * (인기·최신·검색 쿼리는 처음부터 isActive 를 걸고 있어서 더 눈에 띄지 않았다.)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CommunityService - 숨김 글 노출 방지")
class CommunityServiceHiddenPostTest {

    @Mock private PostRepository postRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private TagRepository tagRepository;
    @Mock private UserRepository userRepository;
    @Mock private PostLikeRepository postLikeRepository;
    @Mock private BookmarkRepository bookmarkRepository;
    @Mock private CommunityMapper communityMapper;

    @InjectMocks private CommunityService communityService;

    private Post visiblePost() {
        return Post.builder()
                .id(1L)
                .title("제목")
                .content("내용")
                .author(User.builder().id(10L).userId("user_a").build())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("목록 조회는 활성 글만 읽는 쿼리를 쓴다")
    void listUsesActiveOnlyQuery() {
        Page<Post> page = new PageImpl<>(List.of(visiblePost()));
        when(postRepository.findAllActive(any(Pageable.class))).thenReturn(page);
        when(communityMapper.toPostResponseList(any())).thenReturn(List.of());

        communityService.getAllPosts(0, 10, "createdAt", "DESC");

        verify(postRepository).findAllActive(any(Pageable.class));
        // findAll 로 돌아가면 숨김 글이 목록에 다시 나타난다.
        verify(postRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("숨김 글은 ID 를 알아도 상세가 열리지 않는다")
    void hiddenPostDetailIsNotFound() {
        // 조회수 증가 쿼리 자체가 isActive 조건을 갖고 있어 숨김 글에는 0건이 반영된다.
        when(postRepository.incrementViewCount(1L)).thenReturn(0);

        assertThatThrownBy(() -> communityService.getPostById(1L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(postRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("상세 조회는 활성 글만 읽는 쿼리를 쓴다")
    void detailUsesActiveOnlyQuery() {
        when(postRepository.incrementViewCount(1L)).thenReturn(1);
        when(postRepository.findActiveById(1L)).thenReturn(Optional.of(visiblePost()));

        communityService.getPostById(1L);

        verify(postRepository).findActiveById(1L);
        verify(postRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("조회수는 올랐는데 글이 사라진 경우에도 404 로 끝난다")
    void handlesRaceBetweenIncrementAndFetch() {
        when(postRepository.incrementViewCount(1L)).thenReturn(1);
        when(postRepository.findActiveById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> communityService.getPostById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
