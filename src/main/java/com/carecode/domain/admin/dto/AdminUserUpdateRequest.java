package com.carecode.domain.admin.dto;

import com.carecode.domain.user.entity.UserRole;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 어드민이 변경할 수 있는 사용자 속성만 명시한 요청 객체. 엔티티를 그대로 바인딩하면(과거 @ModelAttribute User) 비밀번호 해시나 임의 필드까지 덮어쓸 */
@Getter
@Setter
@NoArgsConstructor
public class AdminUserUpdateRequest {

    private String name;
    private String phoneNumber;
    private UserRole role;
    private Boolean isActive;
}
