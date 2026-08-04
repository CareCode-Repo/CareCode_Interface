package com.carecode.domain.admin.controller;

import com.carecode.core.exception.ResourceNotFoundException;
import com.carecode.domain.community.dto.response.CommunityPostResponse;
import com.carecode.domain.community.entity.Post;
import com.carecode.domain.community.mapper.CommunityMapper;
import com.carecode.domain.community.repository.PostRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * 어드민 커뮤니티 관리 API.
 *
 * <p>관리자는 게시글을 대신 작성하지 않는다. 모더레이션(조회/삭제)만 제공한다.
 */
@RestController
@RequestMapping("/api/admin/community/posts")
@RequiredArgsConstructor
@Tag(name = "어드민 - 커뮤니티", description = "관리자 전용 게시글 모더레이션 API")
public class AdminCommunityController {

    private final PostRepository postRepository;
    private final CommunityMapper communityMapper;

    @GetMapping
    @Operation(summary = "게시글 목록 조회")
    public ResponseEntity<Page<CommunityPostResponse>> list(
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(postRepository.findAll(pageable).map(communityMapper::toPostResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "게시글 상세 조회")
    public ResponseEntity<CommunityPostResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(communityMapper.toPostResponse(findPost(id)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "게시글 삭제", description = "부적절한 게시글을 관리자 권한으로 삭제합니다.")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        postRepository.delete(findPost(id));
        return ResponseEntity.noContent().build();
    }

    private Post findPost(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다. ID: " + id));
    }
}
