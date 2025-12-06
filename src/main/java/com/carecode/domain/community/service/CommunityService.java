package com.carecode.domain.community.service;

import com.carecode.core.exception.CareServiceException;
import com.carecode.core.exception.ResourceNotFoundException;
import com.carecode.domain.community.dto.request.CommunityRequest;
import com.carecode.domain.community.dto.request.CommunityCreatePostRequest;
import com.carecode.domain.community.dto.request.CommunityUpdatePostRequest;
import com.carecode.domain.community.dto.request.CommunityCreateCommentRequest;
import com.carecode.domain.community.dto.request.CommunityUpdateCommentRequest;
import com.carecode.domain.community.dto.request.CommunityCreateTagRequest;
import com.carecode.domain.community.dto.response.CommunityResponse;
import com.carecode.domain.community.dto.response.CommunityPostResponse;
import com.carecode.domain.community.dto.response.CommunityPostDetailResponse;
import com.carecode.domain.community.dto.response.CommunityCommentResponse;
import com.carecode.domain.community.dto.response.CommunityTagResponse;
import com.carecode.domain.community.dto.response.CommunityPageResponse;
import com.carecode.domain.community.entity.Comment;
import com.carecode.domain.community.entity.Post;
import com.carecode.domain.community.entity.PostCategory;
import com.carecode.domain.community.entity.Tag;
import com.carecode.domain.community.repository.CommentRepository;
import com.carecode.domain.community.repository.PostRepository;
import com.carecode.domain.community.repository.TagRepository;
import com.carecode.domain.community.repository.PostLikeRepository;
import com.carecode.domain.community.repository.BookmarkRepository;
import com.carecode.domain.community.mapper.CommunityMapper;
import com.carecode.domain.community.entity.PostLike;
import com.carecode.domain.community.entity.Bookmark;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
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
    private final PostLikeRepository postLikeRepository;
    private final BookmarkRepository bookmarkRepository;
    private final CommunityMapper communityMapper;
    
    /**
     * 게시글 목록 조회 (페이징)
     */
    public CommunityPageResponse<CommunityPostResponse> getAllPosts(int page, int size, String sortBy, String sortDirection) {
        log.info("게시글 목록 조회 - 페이지: {}, 크기: {}, 정렬: {}, 방향: {}", page, size, sortBy, sortDirection);
        try {
            Sort sort = com.carecode.core.util.SortUtil.createSort(
                sortBy, sortDirection, "createdAt", Sort.Direction.DESC
            );
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Post> postPage = postRepository.findAll(pageable);
            
            List<CommunityPostResponse> postResponses = communityMapper.toPostResponseList(postPage.getContent());
            
            return CommunityPageResponse.<CommunityPostResponse>builder()
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
    
    // 레거시 전체 조회 메서드 제거 (페이징 API로 일원화)
    
    /**
     * 게시글 상세 조회
     */
    public CommunityPostDetailResponse getPostById(Long postId) {
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
    public CommunityPostResponse createPost(CommunityCreatePostRequest request) {
            // 현재 인증된 사용자 가져오기
            User author = getCurrentUser();
            
            // 카테고리 매핑
            PostCategory category = mapCategory(request.getCategory());
            
            Post post = Post.builder()
                    .title(request.getTitle())
                    .content(request.getContent())
                    .category(category)
                    .author(author)
                    .authorName(author.getName())
                    .isAnonymous(request.isAnonymous())
                    .build();
            
            Post savedPost = postRepository.save(post);
            
            // 태그 처리
            if (request.getTags() != null && !request.getTags().isEmpty()) {
                addTagsToPost(savedPost, request.getTags());
            }
            
            return communityMapper.toPostResponse(savedPost);
    }
    
    /**
     * 게시글 수정
     */
    public CommunityPostResponse updatePost(Long postId, CommunityUpdatePostRequest request) {
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
    public List<CommunityCommentResponse> getCommentsByPostId(Long postId) {
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
    public CommunityCommentResponse createComment(Long postId, CommunityCreateCommentRequest request) {
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
    public CommunityCommentResponse updateComment(Long commentId, CommunityUpdateCommentRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("댓글을 찾을 수 없습니다. ID: " + commentId));

        comment.setContent(request.getContent());
        Comment updatedComment = commentRepository.save(comment);

        return communityMapper.toCommentResponse(updatedComment);
    }
    
    /**
     * 댓글 삭제
     */
    public void deleteComment(Long commentId) {
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
    }
    
    // ========== 태그 관련 메서드 ==========
    
    /**
     * 태그 목록 조회
     */
    public List<CommunityTagResponse> getAllTags() {
        List<Tag> tags = tagRepository.findByIsActiveTrue();
        return communityMapper.toTagResponseList(tags);
    }
    
    /**
     * 태그 검색
     */
    public List<CommunityTagResponse> searchTags(String keyword) {
        List<Tag> tags = tagRepository.findByNameContainingAndIsActiveTrue(keyword);
        return communityMapper.toTagResponseList(tags);
    }
    
    /**
     * 태그 생성
     */
    public CommunityTagResponse createTag(CommunityCreateTagRequest request) {
        if (tagRepository.existsByName(request.getName())) {
            throw new CareServiceException("이미 존재하는 태그입니다: " + request.getName());
        }

        Tag tag = Tag.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        Tag savedTag = tagRepository.save(tag);
        return communityMapper.toTagResponse(savedTag);
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
    public List<CommunityTagResponse> getTagsByPostId(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다. ID: " + postId));
        return communityMapper.toTagResponseList(post.getTags());
    }
    
    /**
     * 태그별 게시글 목록 조회
     */
    public List<CommunityPostResponse> getPostsByTagId(Long tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("태그를 찾을 수 없습니다. ID: " + tagId));
        return communityMapper.toPostResponseList(postRepository.findByTagsContaining(tag));
    }

    /**
     * 현재 인증된 사용자 가져오기
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

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
    private PostCategory mapCategory(String category) {
        if (category == null) {
            return PostCategory.GENERAL;
        }
        
        switch (category.toUpperCase()) {
            case "PARENTING":
            case "육아팁":
            case "정보공유":
                return PostCategory.SHARE;
            case "질문":
            case "고민상담":
                return PostCategory.QUESTION;
            case "일상":
            case "GENERAL":
                return PostCategory.GENERAL;
            case "후기":
            case "REVIEW":
                return PostCategory.REVIEW;
            case "뉴스":
            case "NEWS":
                return PostCategory.NEWS;
            case "이벤트":
            case "EVENT":
                return PostCategory.EVENT;
            case "공지사항":
            case "NOTICE":
                return PostCategory.NOTICE;
            default:
                return PostCategory.GENERAL;
        }
    }

    /**
     * 게시글 검색 (페이징)
     */
    public CommunityPageResponse<CommunityPostResponse> searchPosts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Post> postPage = postRepository.findByKeyword(keyword, pageable);

        List<CommunityPostResponse> postResponses = communityMapper.toPostResponseList(postPage.getContent());

        return CommunityPageResponse.<CommunityPostResponse>builder()
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
    }
    
    // 레거시 전체 검색 메서드 제거 (페이징 API로 일원화)

    /**
     * 인기 게시글 조회 (페이징)
     */
    public CommunityPageResponse<CommunityPostResponse> getPopularPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.findPopularPosts(pageable);

        List<CommunityPostResponse> postResponses = communityMapper.toPostResponseList(postPage.getContent());

        return CommunityPageResponse.<CommunityPostResponse>builder()
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
    }

    // 레거시 인기 게시글 리스트 메서드 제거 (페이징 API로 일원화)

    /**
     * 최신 게시글 조회 (페이징)
     */
    public CommunityPageResponse<CommunityPostResponse> getLatestPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.findLatestPosts(pageable);

        List<CommunityPostResponse> postResponses = communityMapper.toPostResponseList(postPage.getContent());

        return CommunityPageResponse.<CommunityPostResponse>builder()
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
    }

    /**
     * 좋아요 토글
     */
    public boolean toggleLike(Long postId, Long userId) {
        log.info("좋아요 토글 - 게시글 ID: {}, 사용자 ID: {}", postId, userId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다: " + postId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        // 이미 좋아요를 눌렀는지 확인
        if (postLikeRepository.existsByPostAndUser(post, user)) {
            // 좋아요 취소
            postLikeRepository.deleteByPostAndUser(post, user);
            log.info("좋아요 취소됨 - 게시글 ID: {}, 사용자 ID: {}", postId, userId);
            return false;
        } else {
            // 좋아요 추가
            PostLike postLike = PostLike.builder()
                    .post(post)
                    .user(user)
                    .build();
            postLikeRepository.save(postLike);
            log.info("좋아요 추가됨 - 게시글 ID: {}, 사용자 ID: {}", postId, userId);
            return true;
        }
    }

    /**
     * 북마크 토글
     */
    public boolean toggleBookmark(Long postId, Long userId) {
        log.info("북마크 토글 - 게시글 ID: {}, 사용자 ID: {}", postId, userId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다: " + postId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        // 이미 북마크했는지 확인
        if (bookmarkRepository.existsByPostAndUser(post, user)) {
            // 북마크 취소
            bookmarkRepository.deleteByPostAndUser(post, user);
            log.info("북마크 취소됨 - 게시글 ID: {}, 사용자 ID: {}", postId, userId);
            return false;
        } else {
            // 북마크 추가
            Bookmark bookmark = Bookmark.builder()
                    .post(post)
                    .user(user)
                    .build();
            bookmarkRepository.save(bookmark);
            log.info("북마크 추가됨 - 게시글 ID: {}, 사용자 ID: {}", postId, userId);
            return true;
        }
    }

    /**
     * 사용자가 특정 게시글에 좋아요를 눌렀는지 확인
     */
    @Transactional(readOnly = true)
    public boolean isLikedByUser(Long postId, Long userId) {
        if (userId == null) {
            return false;
        }

        Post post = postRepository.findById(postId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);

        if (post == null || user == null) {
            return false;
        }

        return postLikeRepository.existsByPostAndUser(post, user);
    }

    /**
     * 사용자가 특정 게시글을 북마크했는지 확인
     */
    @Transactional(readOnly = true)
    public boolean isBookmarkedByUser(Long postId, Long userId) {
        if (userId == null) {
            return false;
        }

        Post post = postRepository.findById(postId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);

        if (post == null || user == null) {
            return false;
        }

        return bookmarkRepository.existsByPostAndUser(post, user);
    }

    /**
     * 특정 게시글의 좋아요 개수 조회
     */
    @Transactional(readOnly = true)
    public long getLikeCount(Long postId) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            return 0;
        }
        return postLikeRepository.countByPost(post);
    }

    /**
     * 특정 게시글의 북마크 개수 조회
     */
    @Transactional(readOnly = true)
    public long getBookmarkCount(Long postId) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            return 0;
        }
        return bookmarkRepository.countByPost(post);
    }

    /**
     * 사용자가 좋아요한 게시글 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CommunityPostResponse> getLikedPosts(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        
        List<PostLike> likes = postLikeRepository.findByUser(user);
        List<Post> posts = likes.stream()
                .map(PostLike::getPost)
                .collect(Collectors.toList());
        
        return communityMapper.toPostResponseList(posts);
    }

    /**
     * 사용자가 북마크한 게시글 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CommunityPostResponse> getBookmarkedPosts(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        
        List<Bookmark> bookmarks = bookmarkRepository.findByUser(user);
        List<Post> posts = bookmarks.stream()
                .map(Bookmark::getPost)
                .collect(Collectors.toList());
        
        return communityMapper.toPostResponseList(posts);
    }

    /**
     * 현재 로그인한 사용자 ID 가져오기
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String username = authentication.getName();
        if (username == null || username.equals("anonymousUser")) {
            return null;
        }

        User user = userRepository.findByEmail(username).orElse(null);
        return user != null ? user.getId() : null;
    }
} 