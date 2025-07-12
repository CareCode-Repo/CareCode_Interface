package com.carecode.domain.community.mapper;

import com.carecode.domain.community.dto.CommunityResponseDto;
import com.carecode.domain.community.entity.Comment;
import com.carecode.domain.community.entity.Post;
import com.carecode.domain.community.entity.Tag;
import com.carecode.domain.community.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 커뮤니티 DTO 변환 매퍼 클래스
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityMapper {
    
    private final CommentRepository commentRepository;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Post 엔티티를 PostResponse DTO로 변환
     */
    public CommunityResponseDto.PostResponse toPostResponse(Post post) {
        List<String> tagNames = post.getTags() != null ? post.getTags().stream().map(Tag::getName).toList() : List.of();
        return CommunityResponseDto.PostResponse.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory().name())
                .authorName(post.getAuthorName())
                .authorId(post.getAuthor().getId().toString())
                .isAnonymous(post.getIsAnonymous())
                .createdAt(post.getCreatedAt().toString())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .tags(tagNames)
                .isLiked(false)
                .isBookmarked(false)
                .build();
    }
    
    /**
     * Post 엔티티를 PostDetailResponse DTO로 변환
     */
    public CommunityResponseDto.PostDetailResponse toPostDetailResponse(Post post) {
        List<Comment> comments = commentRepository.findByPostIdAndParentCommentIsNull(post.getId());
        List<CommunityResponseDto.CommentResponse> commentResponses = comments.stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());
        
        // PostResponse의 기본 필드들 설정
        CommunityResponseDto.PostDetailResponse response = new CommunityResponseDto.PostDetailResponse();
        response.setPostId(post.getId());
        response.setTitle(post.getTitle());
        response.setContent(post.getContent());
        response.setCategory(post.getCategory().name());
        response.setAuthorName(post.getAuthorName());
        response.setAuthorId(post.getAuthor().getId().toString());
        response.setIsAnonymous(post.getIsAnonymous());
        response.setCreatedAt(post.getCreatedAt().format(DATE_FORMATTER));
        response.setViewCount(post.getViewCount());
        response.setLikeCount(post.getLikeCount());
        response.setCommentCount(post.getCommentCount());
        response.setIsLiked(false); // TODO: 실제 좋아요 상태 확인
        response.setIsBookmarked(false); // TODO: 실제 북마크 상태 확인
        
        // PostDetailResponse의 추가 필드들 설정
        response.setComments(commentResponses);
        
        return response;
    }
    
    /**
     * Comment 엔티티를 CommentResponse DTO로 변환
     */
    public CommunityResponseDto.CommentResponse toCommentResponse(Comment comment) {
        List<CommunityResponseDto.CommentResponse> replies = comment.getReplies().stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());
        
        return CommunityResponseDto.CommentResponse.builder()
                .commentId(comment.getId())
                .content(comment.getContent())
                .authorName(comment.getAuthorName())
                .authorId(comment.getAuthor().getId().toString())
                .createdAt(comment.getCreatedAt().format(DATE_FORMATTER))
                .likeCount(comment.getLikeCount())
                .isLiked(false) // TODO: 실제 좋아요 상태 확인
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .replies(replies)
                .build();
    }
    
    /**
     * Tag 엔티티를 TagResponse DTO로 변환
     */
    public CommunityResponseDto.TagResponse toTagResponse(Tag tag) {
        return CommunityResponseDto.TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .description(tag.getDescription())
                .createdAt(tag.getCreatedAt() != null ? tag.getCreatedAt().format(DATE_FORMATTER) : null)
                .build();
    }
    
    /**
     * Post 엔티티 리스트를 PostResponse DTO 리스트로 변환
     */
    public List<CommunityResponseDto.PostResponse> toPostResponseList(List<Post> posts) {
        return posts.stream()
                .map(this::toPostResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Comment 엔티티 리스트를 CommentResponse DTO 리스트로 변환
     */
    public List<CommunityResponseDto.CommentResponse> toCommentResponseList(List<Comment> comments) {
        return comments.stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Tag 엔티티 리스트를 TagResponse DTO 리스트로 변환
     */
    public List<CommunityResponseDto.TagResponse> toTagResponseList(List<Tag> tags) {
        return tags.stream()
                .map(this::toTagResponse)
                .collect(Collectors.toList());
    }
} 