package com.carecode.domain.community.service;

import com.carecode.core.exception.CareServiceException;
import com.carecode.core.exception.ResourceNotFoundException;
import com.carecode.domain.community.dto.CommunityRequestDto;
import com.carecode.domain.community.dto.CommunityResponseDto;
import com.carecode.domain.community.entity.Comment;
import com.carecode.domain.community.entity.Post;
import com.carecode.domain.community.entity.Tag;
import com.carecode.domain.community.repository.CommentRepository;
import com.carecode.domain.community.repository.PostRepository;
import com.carecode.domain.community.repository.TagRepository;
import com.carecode.domain.community.mapper.CommunityMapper;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 커뮤니티 서비스 클래스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommunityService {
    
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final CommunityMapper communityMapper;
    
    /**
     * 게시글 목록 조회 (페이징)
     */
    public CommunityResponseDto.PageResponse<CommunityResponseDto.PostResponse> getAllPosts(int page, int size) {
        log.info("게시글 목록 조회 - 페이지: {}, 크기: {}", page, size);
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<Post> postPage = postRepository.findAll(pageable);
            
            List<CommunityResponseDto.PostResponse> postResponses = communityMapper.toPostResponseList(postPage.getContent());
            
            return CommunityResponseDto.PageResponse.<CommunityResponseDto.PostResponse>builder()
                    .content(postResponses)
                    .page(postPage.getNumber())
                    .size(postPage.getSize())
                    .totalElements(postPage.getTotalElements())
                    .totalPages(postPage.getTotalPages())
                    .first(postPage.isFirst())
                    .last(postPage.isLast())
                    .hasNext(postPage.hasNext())
                    .hasPrevious(postPage.hasPrevious())
                    .build();
        } catch (Exception e) {
            log.error("게시글 목록 조회 중 오류 발생: {}", e.getMessage());
            throw new CareServiceException("게시글 목록을 조회하는 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 게시글 목록 조회 (기존 호환성)
     */
    public List<CommunityResponseDto.PostResponse> getAllPosts() {
        log.info("게시글 목록 조회 (전체)");
        try {
            List<Post> posts = postRepository.findAll(Sort.by("createdAt").descending());
            return communityMapper.toPostResponseList(posts);
        } catch (Exception e) {
            log.error("게시글 목록 조회 중 오류 발생: {}", e.getMessage());
            throw new CareServiceException("게시글 목록을 조회하는 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 게시글 상세 조회
     */
    public CommunityResponseDto.PostDetailResponse getPostById(Long postId) {
        log.info("게시글 상세 조회 - 게시글 ID: {}", postId);
        try {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다. ID: " + postId));
            
            // 조회수 증가
            post.setViewCount(post.getViewCount() + 1);
            postRepository.save(post);
            
            return communityMapper.toPostDetailResponse(post);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("게시글 상세 조회 중 오류 발생: {}", e.getMessage());
            throw new CareServiceException("게시글을 조회하는 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 게시글 작성
     */
    public CommunityResponseDto.PostResponse createPost(CommunityRequestDto.CreatePostRequest request) {
        log.info("게시글 작성 - 제목: {}", request.getTitle());
        try {
            // 현재 인증된 사용자 가져오기
            User author = getCurrentUser();
            
            // 카테고리 매핑
            Post.PostCategory category = mapCategory(request.getCategory());
            
            Post post = Post.builder()
                    .title(request.getTitle())
                    .content(request.getContent())
                    .category(category)
                    .author(author)
                    .authorName(author.getName())
                    .isAnonymous(request.getIsAnonymous() != null ? request.getIsAnonymous() : false)
                    .build();
            
            Post savedPost = postRepository.save(post);
            
            // 태그 처리
            if (request.getTags() != null && !request.getTags().isEmpty()) {
                addTagsToPost(savedPost, request.getTags());
            }
            
            return communityMapper.toPostResponse(savedPost);
        } catch (Exception e) {
            log.error("게시글 작성 중 오류 발생: {}", e.getMessage());
            throw new CareServiceException("게시글을 작성하는 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 게시글 수정
     */
    public CommunityResponseDto.PostResponse updatePost(Long postId, CommunityRequestDto.UpdatePostRequest request) {
        log.info("게시글 수정 - 게시글 ID: {}", postId);
        try {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다. ID: " + postId));
            
            post.setTitle(request.getTitle());
            post.setContent(request.getContent());
            post.setCategory(mapCategory(request.getCategory()));
            
            Post updatedPost = postRepository.save(post);
            return communityMapper.toPostResponse(updatedPost);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("게시글 수정 중 오류 발생: {}", e.getMessage());
            throw new CareServiceException("게시글을 수정하는 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 게시글 삭제
     */
    public void deletePost(Long postId) {
        log.info("게시글 삭제 - 게시글 ID: {}", postId);
        try {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다. ID: " + postId));
            
            postRepository.delete(post);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("게시글 삭제 중 오류 발생: {}", e.getMessage());
            throw new CareServiceException("게시글을 삭제하는 중 오류가 발생했습니다.");
        }
    }
    

    
    // ========== 댓글 관련 메서드 ==========
    
    /**
     * 게시글의 댓글 목록 조회
     */
    public List<CommunityResponseDto.CommentResponse> getCommentsByPostId(Long postId) {
        log.info("댓글 목록 조회 - 게시글 ID: {}", postId);
        try {
            // 게시글 존재 확인
            if (!postRepository.existsById(postId)) {
                throw new ResourceNotFoundException("게시글을 찾을 수 없습니다. ID: " + postId);
            }
            
            List<Comment> comments = commentRepository.findByPostIdAndParentCommentIsNull(postId);
            return communityMapper.toCommentResponseList(comments);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("댓글 목록 조회 중 오류 발생: {}", e.getMessage());
            throw new CareServiceException("댓글 목록을 조회하는 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 댓글 작성
     */
    public CommunityResponseDto.CommentResponse createComment(Long postId, CommunityRequestDto.CreateCommentRequest request) {
        log.info("댓글 작성 - 게시글 ID: {}, 부모 댓글 ID: {}", postId, request.getParentCommentId());
        try {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다. ID: " + postId));
            
            // 현재 인증된 사용자 가져오기
            User author = getCurrentUser();
            
            Comment parentComment = null;
            if (request.getParentCommentId() != null) {
                parentComment = commentRepository.findById(request.getParentCommentId())
                        .orElseThrow(() -> new ResourceNotFoundException("부모 댓글을 찾을 수 없습니다. ID: " + request.getParentCommentId()));
            }
            
            Comment comment = Comment.builder()
                    .post(post)
                    .content(request.getContent())
                    .author(author)
                    .authorName(author.getName())
                    .parentComment(parentComment)
                    .build();
            
            Comment savedComment = commentRepository.save(comment);
            
            // 게시글의 댓글 수 업데이트
            post.setCommentCount((int) commentRepository.countByPostId(postId));
            postRepository.save(post);
            
            return communityMapper.toCommentResponse(savedComment);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("댓글 작성 중 오류 발생: {}", e.getMessage());
            throw new CareServiceException("댓글을 작성하는 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 댓글 수정
     */
    public CommunityResponseDto.CommentResponse updateComment(Long commentId, CommunityRequestDto.UpdateCommentRequest request) {
        log.info("댓글 수정 - 댓글 ID: {}", commentId);
        try {
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new ResourceNotFoundException("댓글을 찾을 수 없습니다. ID: " + commentId));
            
            comment.setContent(request.getContent());
            Comment updatedComment = commentRepository.save(comment);
            
            return communityMapper.toCommentResponse(updatedComment);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("댓글 수정 중 오류 발생: {}", e.getMessage());
            throw new CareServiceException("댓글을 수정하는 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 댓글 삭제
     */
    public void deleteComment(Long commentId) {
        log.info("댓글 삭제 - 댓글 ID: {}", commentId);
        try {
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new ResourceNotFoundException("댓글을 찾을 수 없습니다. ID: " + commentId));
            
            Long postId = comment.getPost().getId();
            commentRepository.delete(comment);
            
            // 게시글의 댓글 수 업데이트
            Post post = postRepository.findById(postId).orElse(null);
            if (post != null) {
                post.setCommentCount((int) commentRepository.countByPostId(postId));
                postRepository.save(post);
            }
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("댓글 삭제 중 오류 발생: {}", e.getMessage());
            throw new CareServiceException("댓글을 삭제하는 중 오류가 발생했습니다.");
        }
    }
    
    // ========== 태그 관련 메서드 ==========
    
    /**
     * 태그 목록 조회
     */
    public List<CommunityResponseDto.TagResponse> getAllTags() {
        log.info("태그 목록 조회");
        try {
            List<Tag> tags = tagRepository.findByIsActiveTrue();
            return communityMapper.toTagResponseList(tags);
        } catch (Exception e) {
            log.error("태그 목록 조회 중 오류 발생: {}", e.getMessage());
            throw new CareServiceException("태그 목록을 조회하는 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 태그 검색
     */
    public List<CommunityResponseDto.TagResponse> searchTags(String keyword) {
        log.info("태그 검색 - 키워드: {}", keyword);
        try {
            List<Tag> tags = tagRepository.findByNameContainingAndIsActiveTrue(keyword);
            return communityMapper.toTagResponseList(tags);
        } catch (Exception e) {
            log.error("태그 검색 중 오류 발생: {}", e.getMessage());
            throw new CareServiceException("태그를 검색하는 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 태그 생성
     */
    public CommunityResponseDto.TagResponse createTag(CommunityRequestDto.CreateTagRequest request) {
        log.info("태그 생성 - 이름: {}", request.getName());
        try {
            if (tagRepository.existsByName(request.getName())) {
                throw new CareServiceException("이미 존재하는 태그입니다: " + request.getName());
            }
            
            Tag tag = Tag.builder()
                    .name(request.getName())
                    .description(request.getDescription())
                    .build();
            
            Tag savedTag = tagRepository.save(tag);
            return communityMapper.toTagResponse(savedTag);
        } catch (CareServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("태그 생성 중 오류 발생: {}", e.getMessage());
            throw new CareServiceException("태그를 생성하는 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 게시글에 태그 추가
     */
    private void addTagsToPost(Post post, List<String> tagNames) {
        for (String tagName : tagNames) {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> createTagIfNotExists(tagName));
            post.addTag(tag);
        }
        postRepository.save(post);
    }
    
    /**
     * 태그가 없으면 생성
     */
    private Tag createTagIfNotExists(String tagName) {
        Tag tag = new Tag(tagName, "자동 생성된 태그");
        return tagRepository.save(tag);
    }
    
    /**
     * 게시글의 태그 목록 조회
     */
    public List<CommunityResponseDto.TagResponse> getTagsByPostId(Long postId) {
        log.info("게시글 태그 목록 조회 - 게시글 ID: {}", postId);
        try {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다. ID: " + postId));
            return communityMapper.toTagResponseList(post.getTags());
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("게시글 태그 목록 조회 중 오류 발생: {}", e.getMessage());
            throw new CareServiceException("게시글 태그 목록을 조회하는 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 태그별 게시글 목록 조회
     */
    public List<CommunityResponseDto.PostResponse> getPostsByTagId(Long tagId) {
        log.info("태그별 게시글 목록 조회 - 태그 ID: {}", tagId);
        try {
            Tag tag = tagRepository.findById(tagId)
                    .orElseThrow(() -> new ResourceNotFoundException("태그를 찾을 수 없습니다. ID: " + tagId));
            return communityMapper.toPostResponseList(postRepository.findByTagsContaining(tag));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("태그별 게시글 목록 조회 중 오류 발생: {}", e.getMessage());
            throw new CareServiceException("태그별 게시글 목록을 조회하는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 현재 인증된 사용자 가져오기
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.debug("getCurrentUser() - Authentication: {}", authentication);
        
        if (authentication == null || !authentication.isAuthenticated() || 
            "anonymousUser".equals(authentication.getName())) {
            log.warn("getCurrentUser() - 인증되지 않은 사용자");
            throw new CareServiceException("인증된 사용자 정보를 찾을 수 없습니다.");
        }
        
        String userEmail = authentication.getName();
        log.info("getCurrentUser() - 인증된 사용자 이메일: {}", userEmail);
        
        // 이메일 형식 검증
        if (userEmail == null || !userEmail.contains("@")) {
            log.error("getCurrentUser() - 유효하지 않은 이메일 형식: {}", userEmail);
            throw new CareServiceException("유효하지 않은 사용자 정보입니다.");
        }

        return userRepository.findByEmailAndDeletedAtIsNull(userEmail)
                .orElseThrow(() -> {
                    log.error("getCurrentUser() - 사용자를 찾을 수 없음: {}", userEmail);
                    return new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userEmail);
                });
    }

    /**
     * 카테고리 매핑 메서드
     */
    private Post.PostCategory mapCategory(String category) {
        if (category == null) {
            return Post.PostCategory.GENERAL;
        }
        
        switch (category.toUpperCase()) {
            case "PARENTING":
            case "육아팁":
            case "정보공유":
                return Post.PostCategory.SHARE;
            case "질문":
            case "고민상담":
                return Post.PostCategory.QUESTION;
            case "일상":
            case "GENERAL":
                return Post.PostCategory.GENERAL;
            case "후기":
            case "REVIEW":
                return Post.PostCategory.REVIEW;
            case "뉴스":
            case "NEWS":
                return Post.PostCategory.NEWS;
            case "이벤트":
            case "EVENT":
                return Post.PostCategory.EVENT;
            case "공지사항":
            case "NOTICE":
                return Post.PostCategory.NOTICE;
            default:
                return Post.PostCategory.GENERAL;
        }
    }

    /**
     * 게시글 검색 (페이징)
     */
    public CommunityResponseDto.PageResponse<CommunityResponseDto.PostResponse> searchPosts(String keyword, int page, int size) {
        log.info("게시글 검색 - 키워드: {}, 페이지: {}, 크기: {}", keyword, page, size);
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<Post> postPage = postRepository.findByKeyword(keyword, pageable);
            
            List<CommunityResponseDto.PostResponse> postResponses = communityMapper.toPostResponseList(postPage.getContent());
            
            return CommunityResponseDto.PageResponse.<CommunityResponseDto.PostResponse>builder()
                    .content(postResponses)
                    .page(postPage.getNumber())
                    .size(postPage.getSize())
                    .totalElements(postPage.getTotalElements())
                    .totalPages(postPage.getTotalPages())
                    .first(postPage.isFirst())
                    .last(postPage.isLast())
                    .hasNext(postPage.hasNext())
                    .hasPrevious(postPage.hasPrevious())
                    .build();
        } catch (Exception e) {
            log.error("게시글 검색 중 오류 발생: {}", e.getMessage());
            throw new CareServiceException("게시글을 검색하는 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 게시글 검색 (기존 호환성)
     */
    public List<CommunityResponseDto.PostResponse> searchPosts(String keyword) {
        log.info("게시글 검색 (전체) - 키워드: {}", keyword);
        try {
            Pageable pageable = PageRequest.of(0, 100); // 최대 100개 반환
            Page<Post> postPage = postRepository.findByKeyword(keyword, pageable);
            return communityMapper.toPostResponseList(postPage.getContent());
        } catch (Exception e) {
            log.error("게시글 검색 중 오류 발생: {}", e.getMessage());
            throw new CareServiceException("게시글을 검색하는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 인기 게시글 조회 (페이징)
     */
    public CommunityResponseDto.PageResponse<CommunityResponseDto.PostResponse> getPopularPosts(int page, int size) {
        log.info("인기 게시글 조회 - 페이지: {}, 크기: {}", page, size);
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Post> postPage = postRepository.findPopularPosts(pageable);
            
            List<CommunityResponseDto.PostResponse> postResponses = communityMapper.toPostResponseList(postPage.getContent());
            
            return CommunityResponseDto.PageResponse.<CommunityResponseDto.PostResponse>builder()
                    .content(postResponses)
                    .page(postPage.getNumber())
                    .size(postPage.getSize())
                    .totalElements(postPage.getTotalElements())
                    .totalPages(postPage.getTotalPages())
                    .first(postPage.isFirst())
                    .last(postPage.isLast())
                    .hasNext(postPage.hasNext())
                    .hasPrevious(postPage.hasPrevious())
                    .build();
        } catch (Exception e) {
            log.error("인기 게시글 조회 중 오류 발생: {}", e.getMessage());
            throw new CareServiceException("인기 게시글을 조회하는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 인기 게시글 조회 (기존 호환성)
     */
    public List<CommunityResponseDto.PostResponse> getPopularPosts(Integer limit) {
        log.info("인기 게시글 조회 - 제한: {}", limit);
        try {
            Pageable pageable = PageRequest.of(0, limit != null ? limit : 10);
            List<Post> posts = postRepository.findPopularPostsList(pageable);
            return communityMapper.toPostResponseList(posts);
        } catch (Exception e) {
            log.error("인기 게시글 조회 중 오류 발생: {}", e.getMessage());
            throw new CareServiceException("인기 게시글을 조회하는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 최신 게시글 조회 (페이징)
     */
    public CommunityResponseDto.PageResponse<CommunityResponseDto.PostResponse> getLatestPosts(int page, int size) {
        log.info("최신 게시글 조회 - 페이지: {}, 크기: {}", page, size);
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Post> postPage = postRepository.findLatestPosts(pageable);
            
            List<CommunityResponseDto.PostResponse> postResponses = communityMapper.toPostResponseList(postPage.getContent());
            
            return CommunityResponseDto.PageResponse.<CommunityResponseDto.PostResponse>builder()
                    .content(postResponses)
                    .page(postPage.getNumber())
                    .size(postPage.getSize())
                    .totalElements(postPage.getTotalElements())
                    .totalPages(postPage.getTotalPages())
                    .first(postPage.isFirst())
                    .last(postPage.isLast())
                    .hasNext(postPage.hasNext())
                    .hasPrevious(postPage.hasPrevious())
                    .build();
        } catch (Exception e) {
            log.error("최신 게시글 조회 중 오류 발생: {}", e.getMessage());
            throw new CareServiceException("최신 게시글을 조회하는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 최신 게시글 조회 (기존 호환성)
     */
    public List<CommunityResponseDto.PostResponse> getLatestPosts(Integer limit) {
        log.info("최신 게시글 조회 - 제한: {}", limit);
        try {
            Pageable pageable = PageRequest.of(0, limit != null ? limit : 10);
            List<Post> posts = postRepository.findLatestPostsList(pageable);
            return communityMapper.toPostResponseList(posts);
        } catch (Exception e) {
            log.error("최신 게시글 조회 중 오류 발생: {}", e.getMessage());
            throw new CareServiceException("최신 게시글을 조회하는 중 오류가 발생했습니다.");
        }
    }
} 