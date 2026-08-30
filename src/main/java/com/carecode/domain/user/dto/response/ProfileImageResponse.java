package com.carecode.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 프로필 이미지 업로드 결과. 저장된 주소를 돌려줘야 클라이언트가 곧바로 화면에 반영할 수 있다. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileImageResponse {
    private String profileImageUrl;
}
