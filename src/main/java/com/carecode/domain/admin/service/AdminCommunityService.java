package com.carecode.domain.admin.service;

import com.carecode.core.exception.ResourceNotFoundException;
import com.carecode.domain.community.dto.response.CommunityPostResponse;
import com.carecode.domain.community.mapper.CommunityMapper;
import com.carecode.domain.community.repository.PostRepository;
import com.carecode.domain.community.service.ModerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 어드민 커뮤니티 모더레이션.
 *
 * <p>조치는 {@link ModerationService} 한 곳을 거친다. 컨트롤러가 리포지토리에 직접 쓰고 있을 때는
 * 같은 의도("이 글은 부적절하다")가 두 결과를 냈다 — 신고 수락은 숨김, 어드민 화면은 하드 삭제.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCommunityService {

    private final PostRepository postRepository;
    private final CommunityMapper communityMapper;
    private final ModerationService moderationService;

    public Page<CommunityPostResponse> list(Pageable pageable) {
        return postRepository.findAll(pageable).map(communityMapper::toPostResponse);
    }

    public CommunityPostResponse detail(Long id) {
        return communityMapper.toPostResponse(postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다. ID: " + id)));
    }

    /**
     * 숨김 처리. 행과 댓글은 남는다 — 되돌릴 수 있어야 하고, 신고 이력이 가리키는 대상도 유지돼야 한다.
     *
     * <p>{@code @Transactional} 이 필요하다. 클래스가 {@code readOnly = true} 라서, 이 메서드에
     * 표시가 없으면 읽기 전용 트랜잭션이 먼저 열리고 {@link ModerationService#hidePost} 의
     * {@code @Transactional}(기본 전파 REQUIRED)이 그 트랜잭션에 참여한다. 읽기 전용에서는
     * Hibernate 가 flush 를 하지 않으므로 <b>UPDATE 가 조용히 사라진다</b> — 응답은 204 인데
     * 숨겨지지 않는다. 실제로 이 상태를 테스트로 먼저 봤다.
     */
    @Transactional
    public void hide(Long id) {
        moderationService.hidePost(id);
    }
}
