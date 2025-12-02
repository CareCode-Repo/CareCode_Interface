package com.carecode.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 위치 기반 사용자 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNearbyUsersResponse {
    private List<UserInfoResponse> users;
    private double centerLatitude;
    private double centerLongitude;
    private double radius;
    private int count;
}

