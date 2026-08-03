package com.carecode.domain.community.dto.request;

import com.carecode.domain.community.entity.Report;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 게시글·댓글 신고 요청.
 */
@Getter
@Setter
@NoArgsConstructor
public class ReportCreateRequest {

    @NotNull(message = "신고 대상 유형은 필수입니다")
    private Report.TargetType targetType;

    @NotNull(message = "신고 대상 ID는 필수입니다")
    private Long targetId;

    @NotNull(message = "신고 사유는 필수입니다")
    private Report.ReportReason reason;

    @Size(max = 1000, message = "상세 내용은 1000자를 넘을 수 없습니다")
    private String detail;
}
