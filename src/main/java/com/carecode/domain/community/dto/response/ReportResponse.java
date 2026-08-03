package com.carecode.domain.community.dto.response;

import com.carecode.domain.community.entity.Report;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 신고 응답.
 *
 * <p>신고자 신원은 관리자에게도 최소한만 노출한다.
 */
@Getter
@Builder
public class ReportResponse {

    private final Long id;
    private final String targetType;
    private final Long targetId;
    private final String reason;
    private final String reasonDisplay;
    private final String detail;
    private final String status;
    private final String moderatorNote;
    private final LocalDateTime createdAt;
    private final LocalDateTime resolvedAt;

    public static ReportResponse from(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .targetType(report.getTargetType().name())
                .targetId(report.getTargetId())
                .reason(report.getReason().name())
                .reasonDisplay(report.getReason().getDisplayName())
                .detail(report.getDetail())
                .status(report.getStatus().name())
                .moderatorNote(report.getModeratorNote())
                .createdAt(report.getCreatedAt())
                .resolvedAt(report.getResolvedAt())
                .build();
    }
}
