package com.carecode.domain.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 채널을 지금 실제로 쓸 수 있는지.
 *
 * 자격증명 미설정이나 사업자 미연동은 서버만 아는 사정이다. 클라이언트가 알 방법이 없어
 * 설정 화면이 "켤 수 있다" 고 안내하면 사용자는 켜 두고 오지 않는 알림을 기다리게 된다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationChannelStatusResponse {

    /** 채널별 설정 변경 API 가 받는 값과 같다. (inapp, email, push, sms) */
    private String channel;

    private String displayName;

    private boolean available;

    /** 쓸 수 없을 때만 채워진다. 화면에 그대로 보여줄 수 있는 문구. */
    private String unavailableReason;

    /**
     * 쓸 수 없는 이유의 종류. 문구는 바뀔 수 있어 클라이언트가 문자열로 판단하면 안 된다.
     *
     * <ul>
     *   <li>{@code SERVER_NOT_CONFIGURED} - 서버 설정 문제. 사용자가 할 수 있는 일이 없다.</li>
     *   <li>{@code NO_DESTINATION} - 보낼 곳이 없다. 사용자가 등록하면 해결된다.</li>
     * </ul>
     */
    private String reasonCode;

    public static final String REASON_SERVER_NOT_CONFIGURED = "SERVER_NOT_CONFIGURED";
    public static final String REASON_NO_DESTINATION = "NO_DESTINATION";
}
