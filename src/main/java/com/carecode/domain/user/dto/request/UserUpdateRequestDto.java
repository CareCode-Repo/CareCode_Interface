package com.carecode.domain.user.dto.request;

import com.carecode.domain.user.entity.Gender;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequestDto {

    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 2, max = 10, message = "이름은 2-10자 사이여야 합니다")
    private String name;

    /**
     * 빈 문자열은 "번호 지우기" 로 받는다. 하이픈 없이 숫자만 넣어도 받아서 하이픈을 붙여 저장한다.
     * 전에는 둘 다 400 이었다. 프런트는 번호를 비워 두면 '' 를 보내므로, 번호 없는 사용자는
     * 이름이나 주소만 고치려 해도 프로필을 저장할 수 없었다.
     */
    @Pattern(regexp = "^$|^01[0-9]-[0-9]{3,4}-[0-9]{4}$", message = "올바른 휴대폰 번호 형식이 아닙니다 (예: 010-1234-5678)")
    private String phoneNumber;

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = normalizePhoneNumber(phoneNumber);
    }

    static String normalizePhoneNumber(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.matches("^01[0-9]{8,9}$")) {
            int middleEnd = trimmed.length() - 4;
            return trimmed.substring(0, 3) + "-" + trimmed.substring(3, middleEnd) + "-" + trimmed.substring(middleEnd);
        }
        return trimmed;
    }

    private LocalDate birthDate;

    private Gender gender;

    @Size(max = 200, message = "주소는 200자 이하여야 합니다")
    private String address;

    private Double latitude;

    private Double longitude;

    /** 가구 소득 / 기준중위소득 (%). 소득 조건이 붙은 지원금 판정에만 쓰며 실제 금액은 받지 않는다. */
    @Min(value = 0, message = "소득 비율은 0 이상이어야 합니다")
    @Max(value = 1000, message = "소득 비율은 1000% 이하여야 합니다")
    private Integer incomePercent;

    @Min(value = 1, message = "가구원 수는 1명 이상이어야 합니다")
    @Max(value = 20, message = "가구원 수는 20명 이하여야 합니다")
    private Integer householdSize;
}

