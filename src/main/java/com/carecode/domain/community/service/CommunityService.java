package com.carecode.domain.community.service;

import com.carecode.core.exception.CareServiceException;
import com.carecode.core.exception.ResourceNotFoundException;
import com.carecode.domain.community.dto.CommunityRequestDto;
import com.carecode.domain.community.dto.CommunityResponseDto;
import com.carecode.domain.community.entity.Comment;
import com.carecode.domain.community.entity.Post;
import com.carecode.domain.community.entity.Tag;
import com.carecode.domain.community.entity.PostTag;
import com.carecode.domain.community.repository.CommentRepository;
import com.carecode.domain.community.repository.PostRepository;
import com.carecode.domain.community.repository.TagRepository;
import com.carecode.domain.community.repository.PostTagRepository;
import com.carecode.domain.community.mapper.CommunityMapper;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final CommunityMapper communityMapper;
    
    /**
     * 게시글 목록 조회
     */
    public List<CommunityResponseDto.PostResponse> getAllPosts() {
        log.info("게시글 목록 조회");
        try {
            List<Post> posts = postRepository.findAll();
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
            // TODO: 실제 인증된 사용자 ID로 변경
            User author = userRepository.findById(1L) // 임시로 ID 1 사용
                    .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
            
            Post post = Post.builder()
                    .title(request.getTitle())
                    .content(request.getContent())
                    .category(Post.PostCategory.valueOf(request.getCategory()))
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
            post.setCategory(Post.PostCategory.valueOf(request.getCategory()));
            
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
                    .map(communityMapper::toPostResponse)
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
            
            // TODO: 실제 인증된 사용자 ID로 변경
            User author = userRepository.findById(1L) // 임시로 ID 1 사용
                    .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
            
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
        Tag tag = Tag.builder()
                .name(tagName)
                .description("자동 생성된 태그")
                .build();
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
} 