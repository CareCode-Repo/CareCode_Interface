package com.carecode.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 이메일 회원가입 요청 DTO.
 *
 * <p>예전에는 응답 DTO 인 {@code UserDto} 를 그대로 요청 본문으로 받았다. 그래서
 * {@code role} / {@code provider} / {@code providerId} / {@code isActive} / {@code emailVerified} 처럼
 * <b>서버가 정해야 할 필드가 전부 요청 계약에 노출</b>돼 있었고, 실제로 {@code role} 을 그대로
 * 저장해 누구나 관리자가 될 수 있었다(#82). 서비스에서 값을 무시하도록 막았지만, Swagger 에는
 * 여전히 설정 가능한 것처럼 보였고 "무시되니까 괜찮다" 는 전제는 언젠가 깨진다.
 *
 * <p>여기에는 가입자가 정할 수 있는 값만 둔다. 요청에 {@code role} 등을 넣어 보내도
 * 받을 필드가 없어 버려진다(스프링 기본값: 모르는 필드 무시).
 *
 * <p>또 {@code UserDto} 에는 검증 어노테이션이 하나도 없어서 {@code @Valid} 가 아무 일도 하지 않았다.
 * 빈 이메일·형식이 틀린 이메일·한 글자 비밀번호로도 가입됐고, 이름이 없으면 DB NOT NULL 제약에서 500 이 났다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignUpRequest {

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "이메일 형식이 올바르지 않습니다")
    @Size(max = 255, message = "이메일이 너무 깁니다")
    private String email;

    /**
     * 상한을 두는 건 BCrypt 가 72바이트 이후를 무시하기 때문이다(그 뒤는 검사되지 않는다).
     * 영문 기준 64자면 안쪽이다. 한글은 글자당 3바이트라 24자를 넘으면 뒷부분이 잘린다.
     */
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 64, message = "비밀번호는 8~64자여야 합니다")
    private String password;

    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 50, message = "이름은 50자를 넘을 수 없습니다")
    private String name;

    @Pattern(regexp = "^[0-9+\\-() ]{0,20}$", message = "전화번호 형식이 올바르지 않습니다")
    private String phoneNumber;

    @Past(message = "생년월일은 과거 날짜여야 합니다")
    private LocalDate birthDate;

    /** Gender.valueOf 에 그대로 넘기므로 enum 이름만 받는다. 다른 값이면 예전에는 500 이었다. */
    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "성별은 MALE, FEMALE, OTHER 중 하나여야 합니다")
    private String gender;

    @Size(max = 255, message = "주소가 너무 깁니다")
    private String address;
}
