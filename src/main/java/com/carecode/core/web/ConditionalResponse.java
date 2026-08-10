package com.carecode.core.web;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.zip.CRC32;

/**
 * 변경되지 않았으면 304 로 응답한다.
 * 소아과 대기실처럼 신호가 약한 곳에서 접종 기록을 확인하는 경우가 많아, 본문 재전송을 줄이면 체감이 크게 다르다.
 */
public final class ConditionalResponse {

    /** 목록은 자주 바뀌지 않지만 오래 캐싱하면 갱신이 늦는다. */
    private static final Duration DEFAULT_MAX_AGE = Duration.ofMinutes(5);

    private ConditionalResponse() {
    }

    /**
     * 내용이 그대로면 304, 바뀌었으면 200 + ETag 를 준다.
     *
     * @param fingerprint 내용이 바뀌면 함께 바뀌는 값 (갱신 시각, 건수 등)
     */
    public static <T> ResponseEntity<T> of(WebRequest request, T body, String fingerprint) {
        String etag = toEtag(fingerprint);

        // checkNotModified 는 일치하면 응답에 304 를 세팅하고 true 를 돌려준다.
        if (request.checkNotModified(etag)) {
            return ResponseEntity.status(304)
                    .eTag(etag)
                    .cacheControl(CacheControl.maxAge(DEFAULT_MAX_AGE).cachePrivate())
                    .build();
        }

        return ResponseEntity.ok()
                .eTag(etag)
                // 개인 데이터라 중간 캐시에 저장되면 안 된다.
                .cacheControl(CacheControl.maxAge(DEFAULT_MAX_AGE).cachePrivate())
                .body(body);
    }

    /** 지문을 짧은 해시로 줄인다. 값이 그대로면 같은 태그가 나온다. */
    private static String toEtag(String fingerprint) {
        CRC32 crc = new CRC32();
        crc.update(fingerprint == null ? new byte[0] : fingerprint.getBytes(StandardCharsets.UTF_8));
        return "\"" + Long.toHexString(crc.getValue()) + "\"";
    }
}
