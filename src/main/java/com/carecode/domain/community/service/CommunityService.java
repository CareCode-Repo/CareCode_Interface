package com.carecode.domain.community.service;

import com.carecode.core.exception.CareServiceException;
import com.carecode.core.exception.CommentAccessDeniedException;
import com.carecode.core.exception.PostAccessDeniedException;
import com.carecode.core.exception.ResourceNotFoundException;
import com.carecode.core.security.CurrentUserFacade;
import com.carecode.domain.community.dto.request.CommunityCreatePostRequest;
import com.carecode.domain.community.dto.request.CommunityUpdatePostRequest;
import com.carecode.domain.community.dto.request.CommunityCreateCommentRequest;
import com.carecode.domain.community.dto.request.CommunityUpdateCommentRequest;
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
import com.carecode.domain.user.entity.UserRole;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** 커뮤니티 서비스 클래스 */
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
    private final CurrentUserFacade currentUserFacade;

    static final String ANONYMOUS_AUTHOR_NAME = "익명";

    /**
     * 게시글 목록 조회 (페이징).
     *
     * <p>findAll 이 아니라 findAllActive 다. 예전에는 필터 없이 전부 읽어서, 신고 누적으로
     * 자동 숨김된 글(ModerationService)이 목록 첫 페이지에 그대로 남아 있었다.
     * 인기·최신·검색 쿼리는 처음부터 isActive 를 걸고 있었는데 기본 목록만 빠져 있었다.
     */
    @Transactional(readOnly = true)
    public CommunityPageResponse<CommunityPostResponse> getAllPosts(int page, int size, String sortBy, String sortDirection) {
        log.info("게시글 목록 조회 - 페이지: {}, 크기: {}, 정렬: {}, 방향: {}", page, size, sortBy, sortDirection);

        Sort sort = com.carecode.core.util.SortUtil.createSort(
            sortBy, sortDirection, "createdAt", Sort.Direction.DESC
        );
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Post> postPage = postRepository.findAllActive(pageable);

        List<CommunityPostResponse> postResponses = present(postPage.getContent());

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
    
    // 레거시 전체 조회 메서드 제거 (페이징 API로 일원화)

    /**
     * 게시글 상세 조회.
     *
     * <p>숨김 처리된 글은 ID 를 알아도 열리지 않아야 한다. 목록에서만 감추고 상세를 열어두면
     * 링크가 이미 퍼진 글에 대해서는 숨김이 아무 효과가 없다.
     * (increment 쿼리 자체가 isActive 조건을 갖고 있어 숨김 글은 조회수도 오르지 않는다.)
     */
    public CommunityPostDetailResponse getPostById(Long postId) {
        log.info("게시글 상세 조회 - 게시글 ID: {}", postId);

        // 조회수는 DB 에서 원자적으로 증가시킨다 (동시 조회 시 증가분 유실 방지).
        int updated = postRepository.incrementViewCount(postId);
        if (updated == 0) {
            throw new ResourceNotFoundException("게시글을 찾을 수 없습니다. ID: " + postId);
        }

        Post post = postRepository.findActiveById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다. ID: " + postId));

        CommunityPostDetailResponse response = communityMapper.toPostDetailResponse(post);
        if (response != null) {
            applyViewerState(List.of(post), List.of(response));
        }
        return response;
    }

    // 게시글 작성
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
            
            return present(List.of(savedPost)).get(0);
    }

    // 게시글 수정
    public CommunityPostResponse updatePost(Long postId, CommunityUpdatePostRequest request) {
        log.info("게시글 수정 - 게시글 ID: {}", postId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다. ID: " + postId));

        requirePostOwnership(post);

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setCategory(mapCategory(request.getCategory()));

        Post updatedPost = postRepository.save(post);
        return present(List.of(updatedPost)).get(0);
    }

    // 게시글 삭제
    public void deletePost(Long postId) {
        log.info("게시글 삭제 - 게시글 ID: {}", postId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다. ID: " + postId));

        requirePostOwnership(post);

        postRepository.delete(post);
    }

    
    // ==========
    // 댓글 관련 메서드 ==========

    // 게시글의 댓글 목록 조회
    public List<CommunityCommentResponse> getCommentsByPostId(Long postId) {
        log.info("댓글 목록 조회 - 게시글 ID: {}", postId);
        try {
            // 게시글 존재 확인
            if (!postRepository.existsById(postId)) {
                throw new ResourceNotFoundException("게시글을 찾을 수 없습니다. ID: " + postId);
            }
            
            // 답글까지 한 번에 읽고 트리는 메모리에서 조립한다 (댓글 수만큼 쿼리가 나가던 경로).
            List<Comment> comments = commentRepository.findActiveTreeByPostId(postId);
            return communityMapper.toCommentTree(comments);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("댓글 목록 조회 중 오류 발생: {}", e.getMessage());
            throw new CareServiceException("댓글 목록을 조회하는 중 오류가 발생했습니다.");
        }
    }

    // 댓글 작성
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

    // 댓글 수정
    public CommunityCommentResponse updateComment(Long commentId, CommunityUpdateCommentRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("댓글을 찾을 수 없습니다. ID: " + commentId));

        requireCommentOwnership(comment);

        comment.setContent(request.getContent());
        Comment updatedComment = commentRepository.save(comment);

        return communityMapper.toCommentResponse(updatedComment);
    }

    // 댓글 삭제
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("댓글을 찾을 수 없습니다. ID: " + commentId));

        requireCommentOwnership(comment);

        Long postId = comment.getPost().getId();
        commentRepository.delete(comment);

        // 게시글의 댓글 수 업데이트
        Post post = postRepository.findById(postId).orElse(null);
        if (post != null) {
            post.setCommentCount((int) commentRepository.countByPostId(postId));
            postRepository.save(post);
        }
    }
    
    // ==========
    // 태그 관련 메서드 ==========

    // 태그 목록 조회
    public List<CommunityTagResponse> getAllTags() {
        List<Tag> tags = tagRepository.findByIsActiveTrue();
        return communityMapper.toTagResponseList(tags);
    }

    // 게시글에 태그 추가
    private void addTagsToPost(Post post, List<String> tagNames) {
        for (String tagName : tagNames) {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> createTagIfNotExists(tagName));
            post.addTag(tag);
        }
        postRepository.save(post);
    }

    // 태그가 없으면 생성
    private Tag createTagIfNotExists(String tagName) {
        Tag tag = new Tag(tagName, "자동 생성된 태그");
        return tagRepository.save(tag);
    }

    // 현재 인증된 사용자 가져오기
    /**
     * 게시글 응답에 "보는 사람" 기준 상태를 입힌다.
     *
     * <ul>
     *   <li>좋아요·북마크 여부 — 매퍼는 사용자를 모르므로 늘 false 였다. 목록 전체를 쿼리 두 번으로 채운다.</li>
     *   <li>익명 글 작성자 가리기 — 전에는 익명 글에도 실명과 작성자 ID 가 그대로 나갔다.
     *       화면만 "익명" 으로 바꿔 보여 줬을 뿐 응답을 보면 누구 글인지 알 수 있었다.</li>
     * </ul>
     */
    private List<CommunityPostResponse> present(List<Post> posts) {
        List<CommunityPostResponse> responses = communityMapper.toPostResponseList(posts);
        applyViewerState(posts, responses);
        return responses;
    }

    private void applyViewerState(List<Post> posts, List<? extends CommunityPostResponse> responses) {
        User viewer = currentUserFacade.findCurrentUser().orElse(null);
        Set<Long> liked = Set.of();
        Set<Long> bookmarked = Set.of();
        if (viewer != null && !posts.isEmpty()) {
            List<Long> ids = posts.stream().map(Post::getId).toList();
            liked = new HashSet<>(postLikeRepository.findLikedPostIdsByUserAndPostIds(viewer, ids));
            bookmarked = new HashSet<>(bookmarkRepository.findBookmarkedPostIdsByUserAndPostIds(viewer, ids));
        }
        for (int i = 0; i < Math.min(posts.size(), responses.size()); i++) {
            Post post = posts.get(i);
            CommunityPostResponse response = responses.get(i);
            if (response == null) {
                continue;
            }
            response.setIsLiked(liked.contains(post.getId()));
            response.setIsBookmarked(bookmarked.contains(post.getId()));
            maskAnonymousAuthor(post, response, viewer);
        }
    }

    /**
     * 익명 글이면 이름을 가린다. 작성자 ID 는 본인에게만 준다(수정·삭제 버튼 판단용).
     * 남에게 ID 를 주면 같은 사용자의 실명 글과 이어 붙여 누구인지 알아낼 수 있다.
     * 프런트 스키마가 authorId 를 필수 문자열로 받으므로 null 대신 빈 문자열을 준다.
     */
    static void maskAnonymousAuthor(Post post, CommunityPostResponse response, User viewer) {
        if (!Boolean.TRUE.equals(post.getIsAnonymous())) {
            return;
        }
        response.setAuthorName(ANONYMOUS_AUTHOR_NAME);
        boolean viewerIsAuthor = viewer != null && post.getAuthor() != null
                && Objects.equals(viewer.getId(), post.getAuthor().getId());
        if (!viewerIsAuthor) {
            response.setAuthorId("");
        }
    }

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

    // 게시글 소유권 검증 (작성자 본인 또는 관리자만 허용)
    private void requirePostOwnership(Post post) {
        User currentUser = getCurrentUser();
        if (isOwner(post.getAuthor(), currentUser) || isAdmin(currentUser)) {
            return;
        }
        log.warn("게시글 접근 거부 - 게시글ID={}, 요청자ID={}", post.getId(), currentUser.getId());
        throw new PostAccessDeniedException("본인이 작성한 게시글만 수정/삭제할 수 있습니다.");
    }

    // 댓글 소유권 검증 (작성자 본인 또는 관리자만 허용)
    private void requireCommentOwnership(Comment comment) {
        User currentUser = getCurrentUser();
        if (isOwner(comment.getAuthor(), currentUser) || isAdmin(currentUser)) {
            return;
        }
        log.warn("댓글 접근 거부 - 댓글ID={}, 요청자ID={}", comment.getId(), currentUser.getId());
        throw new CommentAccessDeniedException("본인이 작성한 댓글만 수정/삭제할 수 있습니다.");
    }

    private boolean isOwner(User author, User currentUser) {
        return author != null
                && author.getId() != null
                && author.getId().equals(currentUser.getId());
    }

    private boolean isAdmin(User user) {
        return UserRole.ADMIN == user.getRole();
    }

    // 카테고리 매핑 메서드
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

    // 게시글 검색 (페이징)
    public CommunityPageResponse<CommunityPostResponse> searchPosts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Post> postPage = postRepository.findByKeyword(keyword, pageable);

        List<CommunityPostResponse> postResponses = present(postPage.getContent());

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

    // 인기 게시글 조회 (페이징)
    public CommunityPageResponse<CommunityPostResponse> getPopularPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.findPopularPosts(pageable);

        List<CommunityPostResponse> postResponses = present(postPage.getContent());

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

    // 최신 게시글 조회 (페이징)
    public CommunityPageResponse<CommunityPostResponse> getLatestPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.findLatestPosts(pageable);

        List<CommunityPostResponse> postResponses = present(postPage.getContent());

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

    // 좋아요 토글
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
            postRepository.syncLikeCount(postId);
            return false;
        } else {
            // 좋아요 추가
            PostLike postLike = PostLike.builder()
                    .post(post)
                    .user(user)
                    .build();
            postLikeRepository.save(postLike);
            log.info("좋아요 추가됨 - 게시글 ID: {}, 사용자 ID: {}", postId, userId);
            postRepository.syncLikeCount(postId);
            return true;
        }
    }

    // 북마크 토글
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

    // 특정 게시글의 좋아요 개수 조회
    @Transactional(readOnly = true)
    public long getLikeCount(Long postId) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            return 0;
        }
        return postLikeRepository.countByPost(post);
    }

    // 특정 게시글의 북마크 개수 조회
    @Transactional(readOnly = true)
    public long getBookmarkCount(Long postId) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            return 0;
        }
        return bookmarkRepository.countByPost(post);
    }

    // 사용자가 좋아요한 게시글 목록 조회
    @Transactional(readOnly = true)
    public List<CommunityPostResponse> getLikedPosts(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        
        List<PostLike> likes = postLikeRepository.findByUser(user);
        List<Post> posts = likes.stream()
                .map(PostLike::getPost)
                .collect(Collectors.toList());
        
        return present(posts);
    }

    // 사용자가 북마크한 게시글 목록 조회
    @Transactional(readOnly = true)
    public List<CommunityPostResponse> getBookmarkedPosts(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        
        List<Bookmark> bookmarks = bookmarkRepository.findByUser(user);
        List<Post> posts = bookmarks.stream()
                .map(Bookmark::getPost)
                .collect(Collectors.toList());
        
        return present(posts);
    }

    @Transactional(readOnly = true)
    public Long getCurrentAuthenticatedUserId() {
        return getCurrentUser().getId();
    }
} 