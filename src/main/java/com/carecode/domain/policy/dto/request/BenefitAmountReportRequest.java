package com.carecode.domain.policy.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** 실제로 받은 금액 제보. */
@Getter
@Setter
@NoArgsConstructor
public class BenefitAmountReportRequest {

    @NotNull(message = "수령액은 필수입니다")
    @Min(value = 0, message = "수령액은 0 이상이어야 합니다")
    // 육아 지원금에 1억을 넘는 항목은 없다. 자릿수 오입력을 여기서 막는다.
    @Max(value = 100_000_000, message = "수령액이 너무 큽니다. 자릿수를 확인해 주세요")
    private Integer amount;

    @NotNull(message = "지급 방식은 필수입니다")
    @Pattern(regexp = "MONTHLY|ONE_TIME", message = "지급 방식은 MONTHLY 또는 ONE_TIME 이어야 합니다")
    private String paymentType;

    private LocalDate receivedAt;

    @Size(max = 300, message = "메모는 300자 이하여야 합니다")
    private String note;
}
