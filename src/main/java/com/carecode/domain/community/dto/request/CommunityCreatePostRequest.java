package com.carecode.domain.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** 게시글 작성 요청 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityCreatePostRequest {
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다") // TITLE VARCHAR(200)
    private String title;
    
    @NotBlank(message = "내용은 필수입니다")
    @Size(max = 10000, message = "내용은 10000자 이하여야 합니다") // CONTENT TEXT(64KB) 안쪽
    private String content;
    
    private String category;
    private List<String> tags;
    // Lombok 이 primitive boolean isX 의 접근자를 isX()/setX() 로 만들어 JSON 키가 "anonymous" 가 된다.
    // 프런트는 "isAnonymous" 를 보내므로 값이 버려져 익명으로 쓴 글이 실명으로 올라갔다.
    @JsonProperty("isAnonymous")
    @JsonAlias("anonymous")
    private boolean isAnonymous;
}

