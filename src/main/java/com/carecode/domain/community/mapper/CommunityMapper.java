package com.carecode.domain.community.mapper;

import com.carecode.domain.community.dto.response.CommunityPostResponse;
import com.carecode.domain.community.dto.response.CommunityPostDetailResponse;
import com.carecode.domain.community.dto.response.CommunityCommentResponse;
import com.carecode.domain.community.dto.response.CommunityTagResponse;
import com.carecode.domain.community.entity.Comment;
import com.carecode.domain.community.entity.Post;
import com.carecode.domain.community.entity.Tag;
import com.carecode.domain.community.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 커뮤니티 DTO 변환 매퍼 클래스 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityMapper {

    private final CommentRepository commentRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Post 엔티티를 PostResponse DTO로 변환
    public CommunityPostResponse toPostResponse(Post post) {
        List<String> tagNames = post.getTags() != null ? post.getTags().stream().map(Tag::getName).toList() : List.of();
        return CommunityPostResponse.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory().name())
                .authorName(post.getAuthorName())
                .authorId(authorId(post))
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

    // Post 엔티티를 PostDetailResponse DTO로 변환
    public CommunityPostDetailResponse toPostDetailResponse(Post post) {
        // 활성 댓글 전체를 한 번에 읽고 트리는 메모리에서 만든다.
        // 예전에는 최상위 댓글만 읽고 답글을 재귀로 따라가서 노드 수만큼 쿼리가 나갔다.
        List<CommunityCommentResponse> commentResponses =
                toCommentTree(commentRepository.findActiveTreeByPostId(post.getId()));

        // PostResponse의 기본 필드들 설정
        CommunityPostDetailResponse response = new CommunityPostDetailResponse();
        response.setPostId(post.getId());
        response.setTitle(post.getTitle());
        response.setContent(post.getContent());
        response.setCategory(post.getCategory().name());
        response.setAuthorName(post.getAuthorName());
        response.setAuthorId(authorId(post));
        response.setIsAnonymous(post.getIsAnonymous());
        response.setCreatedAt(post.getCreatedAt().format(DATE_FORMATTER));
        response.setViewCount(post.getViewCount());
        response.setLikeCount(post.getLikeCount());
        response.setCommentCount(post.getCommentCount());
        response.setIsLiked(false); // Service 레벨에서 사용자별로 업데이트 필요 (CommunityService.isLikedByUser 참조)
        response.setIsBookmarked(false); // Service 레벨에서 사용자별로 업데이트 필요 (CommunityService.isBookmarkedByUser 참조)

        // PostDetailResponse의 추가 필드들 설정
        response.setComments(commentResponses);

        return response;
    }

    /**
     * 평평한 댓글 목록을 부모-자식 트리로 조립한다.
     *
     * <p>입력은 한 게시글의 활성 댓글 전부다(답글 포함). 부모가 숨김 처리되어 목록에 없으면
     * 답글이 통째로 사라지므로, 그런 고아 답글은 최상위로 올려 응답에서 잃지 않게 한다.
     */
    public List<CommunityCommentResponse> toCommentTree(List<Comment> flatComments) {
        if (flatComments == null || flatComments.isEmpty()) {
            return List.of();
        }

        Map<Long, CommunityCommentResponse> byId = new LinkedHashMap<>();
        for (Comment comment : flatComments) {
            byId.put(comment.getId(), toCommentResponse(comment));
        }

        List<CommunityCommentResponse> roots = new ArrayList<>();
        for (Comment comment : flatComments) {
            CommunityCommentResponse dto = byId.get(comment.getId());
            Long parentId = comment.getParentComment() != null ? comment.getParentComment().getId() : null;

            if (parentId == null) {
                roots.add(dto);
                continue;
            }

            CommunityCommentResponse parent = byId.get(parentId);
            if (parent != null) {
                parent.getReplies().add(dto);
            } else {
                roots.add(dto);
            }
        }
        return roots;
    }

    /**
     * Comment 엔티티를 CommentResponse DTO로 변환한다. 답글은 채우지 않는다.
     *
     * <p>답글을 여기서 재귀로 채우면 지연 로딩 컬렉션을 노드마다 건드려 N+1 이 된다.
     * 트리가 필요한 곳은 {@link #toCommentTree(List)} 를 쓴다.
     */
    public CommunityCommentResponse toCommentResponse(Comment comment) {
        return CommunityCommentResponse.builder()
                .commentId(comment.getId())
                .content(comment.getContent())
                .authorName(comment.getAuthorName())
                .authorId(comment.getAuthor() != null && comment.getAuthor().getId() != null
                        ? comment.getAuthor().getId().toString() : null)
                .createdAt(comment.getCreatedAt().format(DATE_FORMATTER))
                .likeCount(comment.getLikeCount())
                .isLiked(false) // Service 레벨에서 사용자별로 업데이트 필요
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .replies(new ArrayList<>())
                .build();
    }

    // Tag 엔티티를 TagResponse DTO로 변환
    public CommunityTagResponse toTagResponse(Tag tag) {
        return CommunityTagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .description(tag.getDescription())
                .createdAt(tag.getCreatedAt() != null ? tag.getCreatedAt().format(DATE_FORMATTER) : null)
                .build();
    }

    // Post 엔티티 리스트를 PostResponse DTO 리스트로 변환
    public List<CommunityPostResponse> toPostResponseList(List<Post> posts) {
        return posts.stream()
                .map(this::toPostResponse)
                .collect(Collectors.toList());
    }

    // Comment 엔티티 리스트를 CommentResponse DTO 리스트로 변환 (평면)
    public List<CommunityCommentResponse> toCommentResponseList(List<Comment> comments) {
        return comments.stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());
    }

    // Tag 엔티티 리스트를 TagResponse DTO 리스트로 변환
    public List<CommunityTagResponse> toTagResponseList(List<Tag> tags) {
        return tags.stream()
                .map(this::toTagResponse)
                .collect(Collectors.toList());
    }

    /** 익명 글이라도 작성자 식별자는 소유권 판정에 쓰이므로 그대로 내려준다. */
    private String authorId(Post post) {
        return post.getAuthor() != null && post.getAuthor().getId() != null
                ? post.getAuthor().getId().toString()
                : null;
    }
}
