package com.carecode.domain.health.growth;

import java.util.Locale;
import java.util.Optional;

/**
 * 성장 표준 조회를 위한 성별.
 *
 * <p>아이 엔티티의 gender 는 자유 문자열이라 다양한 표기가 들어올 수 있어 관대하게 파싱한다.
 */
public enum Sex {
    MALE,
    FEMALE;

    public static Optional<Sex> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "m", "male", "남", "남자", "남아", "boy" -> Optional.of(MALE);
            case "f", "female", "여", "여자", "여아", "girl" -> Optional.of(FEMALE);
            default -> Optional.empty();
        };
    }
}
