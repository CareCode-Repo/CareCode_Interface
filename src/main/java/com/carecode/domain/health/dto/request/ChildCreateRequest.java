package com.carecode.domain.health.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** 아이 등록/수정 요청. */
@Getter
@Setter
@NoArgsConstructor
public class ChildCreateRequest {

    @NotBlank(message = "아이 이름은 필수입니다")
    @Size(max = 100, message = "이름은 100자를 넘을 수 없습니다")
    private String name;

    @NotNull(message = "생년월일은 필수입니다")
    @PastOrPresent(message = "생년월일은 오늘 이전이어야 합니다")
    private LocalDate birthDate;

    @Size(max = 10)
    private String gender;

    @Size(max = 500)
    private String specialNeeds;
}
