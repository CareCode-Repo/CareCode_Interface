package com.carecode.domain.community.service;

import com.carecode.core.exception.CareServiceException;
import com.carecode.core.exception.ResourceNotFoundException;
import com.carecode.domain.community.dto.CommunityRequestDto;
import com.carecode.domain.community.dto.CommunityResponseDto;
import com.carecode.domain.community.entity.Comment;
import com.carecode.domain.community.entity.Post;
import com.carecode.domain.community.repository.CommentRepository;
import com.carecode.domain.community.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

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
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 게시글 목록 조회
     */
    public List<CommunityResponseDto.PostResponse> getAllPosts() {
        log.info("게시글 목록 조회");
        try {
            List<Post> posts = postRepository.findAll();
            return posts.stream()
                    .map(this::convertToPostResponse)
                    .collect(Collectors.toList());
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
            
            return convertToPostDetailResponse(post);
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
            Post post = Post.builder()
                    .title(request.getTitle())
                    .content(request.getContent())
                    .category(request.getCategory())
                    .authorId("current-user-id") // TODO: 실제 인증된 사용자 ID로 변경
                    .authorName("작성자") // TODO: 실제 사용자 이름으로 변경
                    .isAnonymous(request.getIsAnonymous() != null ? request.getIsAnonymous() : false)
                    .build();
            
            Post savedPost = postRepository.save(post);
            return convertToPostResponse(savedPost);
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
            post.setCategory(request.getCategory());
            
            Post updatedPost = postRepository.save(post);
            return convertToPostResponse(updatedPost);
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
    
    /**
     * 게시글 검색
     */
    public List<CommunityResponseDto.PostResponse> searchPosts(String keyword) {
        log.info("게시글 검색 - 키워드: {}", keyword);
        try {
            // TODO: 실제 검색 로직 구현 (제목, 내용에서 키워드 검색)
            List<Post> posts = postRepository.findAll();
            return posts.stream()
                    .filter(post -> post.getTitle().contains(keyword) || post.getContent().contains(keyword))
                    .map(this::convertToPostResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("게시글 검색 중 오류 발생: {}", e.getMessage());
            throw new CareServiceException("게시글을 검색하는 중 오류가 발생했습니다.");
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
            return comments.stream()
                    .map(this::convertToCommentResponse)
                    .collect(Collectors.toList());
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
            
            Comment parentComment = null;
            if (request.getParentCommentId() != null) {
                parentComment = commentRepository.findById(request.getParentCommentId())
                        .orElseThrow(() -> new ResourceNotFoundException("부모 댓글을 찾을 수 없습니다. ID: " + request.getParentCommentId()));
            }
            
            Comment comment = Comment.builder()
                    .post(post)
                    .content(request.getContent())
                    .authorId("current-user-id") // TODO: 실제 인증된 사용자 ID로 변경
                    .authorName("댓글 작성자") // TODO: 실제 사용자 이름으로 변경
                    .parentComment(parentComment)
                    .build();
            
            Comment savedComment = commentRepository.save(comment);
            
            // 게시글의 댓글 수 업데이트
            post.setCommentCount((int) commentRepository.countByPostId(postId));
            postRepository.save(post);
            
            return convertToCommentResponse(savedComment);
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
            
            return convertToCommentResponse(updatedComment);
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
    
    // ========== DTO 변환 메서드 ==========
    
    /**
     * Post 엔티티를 PostResponse DTO로 변환
     */
    private CommunityResponseDto.PostResponse convertToPostResponse(Post post) {
        return CommunityResponseDto.PostResponse.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory())
                .authorName(post.getAuthorName())
                .authorId(post.getAuthorId())
                .isAnonymous(post.getIsAnonymous())
                .createdAt(post.getCreatedAt().format(DATE_FORMATTER))
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .isLiked(false) // TODO: 실제 좋아요 상태 확인
                .isBookmarked(false) // TODO: 실제 북마크 상태 확인
                .build();
    }
    
    /**
     * Post 엔티티를 PostDetailResponse DTO로 변환
     */
    private CommunityResponseDto.PostDetailResponse convertToPostDetailResponse(Post post) {
        List<Comment> comments = commentRepository.findByPostIdAndParentCommentIsNull(post.getId());
        List<CommunityResponseDto.CommentResponse> commentResponses = comments.stream()
                .map(this::convertToCommentResponse)
                .collect(Collectors.toList());
        
        return CommunityResponseDto.PostDetailResponse.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory())
                .authorName(post.getAuthorName())
                .authorId(post.getAuthorId())
                .isAnonymous(post.getIsAnonymous())
                .createdAt(post.getCreatedAt().format(DATE_FORMATTER))
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .isLiked(false) // TODO: 실제 좋아요 상태 확인
                .isBookmarked(false) // TODO: 실제 북마크 상태 확인
                .comments(commentResponses)
                .build();
    }
    
    /**
     * Comment 엔티티를 CommentResponse DTO로 변환
     */
    private CommunityResponseDto.CommentResponse convertToCommentResponse(Comment comment) {
        List<CommunityResponseDto.CommentResponse> replies = comment.getReplies().stream()
                .map(this::convertToCommentResponse)
                .collect(Collectors.toList());
        
        return CommunityResponseDto.CommentResponse.builder()
                .commentId(comment.getId())
                .content(comment.getContent())
                .authorName(comment.getAuthorName())
                .authorId(comment.getAuthorId())
                .createdAt(comment.getCreatedAt().format(DATE_FORMATTER))
                .likeCount(comment.getLikeCount())
                .isLiked(false) // TODO: 실제 좋아요 상태 확인
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .replies(replies)
                .build();
    }
} 