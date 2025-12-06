package com.carecode.domain.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 알림 검색 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSearchResponse {
    private List<NotificationInfoResponse> notifications;
    private long totalCount;
    private String searchKeyword;
    private List<String> searchFilters;
    private String sortBy;
    private String sortDirection;
}

