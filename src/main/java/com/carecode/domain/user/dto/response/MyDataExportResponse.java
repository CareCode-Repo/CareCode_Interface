package com.carecode.domain.user.dto.response;

import com.carecode.domain.user.entity.Child;
import com.carecode.domain.user.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 내 데이터 전체 내려받기(열람권 행사) 응답.
 *
 * <p>비밀번호·토큰 등 인증 정보는 담지 않는다. 열람권은 "내 정보를 본다" 는 권리이지 인증
 * 수단을 받아 가는 것이 아니고, 내려받은 파일이 그대로 유출되면 계정이 넘어간다.
 *
 * <p>건강 기록과 게시글은 **건수만** 담는다. 본문까지 넣으면 응답이 수 MB 가 되고, 개별 내용은
 * 각 도메인 API 로 이미 열람할 수 있다. 이 응답의 목적은 "무엇이 얼마나 보관돼 있는지" 를
 * 한 번에 보여 주는 것이다.
 */
public record MyDataExportResponse(
        LocalDateTime exportedAt,
        Profile profile,
        List<ChildData> children,
        int healthRecordCount,
        long postCount,
        List<ConsentStatusResponse.ConsentHistoryItem> consentHistory) {

    /** 가입자 본인 정보. */
    public record Profile(
            String userId,
            String email,
            String name,
            String phoneNumber,
            LocalDate birthDate,
            String address,
            String role,
            LocalDateTime createdAt,
            LocalDateTime lastLoginAt) {

        public static Profile from(User user) {
            return new Profile(
                    user.getUserId(),
                    user.getEmail(),
                    user.getName(),
                    user.getPhoneNumber(),
                    user.getBirthDate(),
                    user.getAddress(),
                    user.getRole() != null ? user.getRole().name() : null,
                    user.getCreatedAt(),
                    user.getLastLoginAt());
        }
    }

    /**
     * 자녀 정보.
     *
     * <p>{@code birthDate}·{@code gender} 는 문자열이다. 기존 응답이 {@code String.valueOf()} 로
     * 만든 값을 내보내고 있었고(미등록이면 {@code "null"} 문자열), 프런트가 그대로 표시하므로
     * 타입만 바꾸면 화면이 달라진다. 모양은 그대로 두고 {@code null} 은 {@code null} 로 보낸다.
     */
    public record ChildData(String name, String birthDate, String gender) {

        public static ChildData from(Child child) {
            return new ChildData(
                    child.getName(),
                    child.getBirthDate() != null ? child.getBirthDate().toString() : null,
                    child.getGender());
        }
    }
}
