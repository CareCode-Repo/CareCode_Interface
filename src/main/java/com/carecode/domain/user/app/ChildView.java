package com.carecode.domain.user.app;

import com.carecode.domain.user.entity.Child;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 다른 도메인이 보는 자녀. 엔티티가 아니라 읽기 전용 값이다.
 *
 * <p>정책 추천·지원금 비교·대기 등록은 모두 자녀의 "월령" 으로 판단한다. 그런데 그 계산이
 * {@code ChronoUnit.MONTHS.between(child.getBirthDate(), today)} 로 다섯 군데에 각자 복제돼
 * 있었고, 생일이 없을 때의 처리도 곳마다 달랐다(어떤 곳은 제외, 어떤 곳은 0). 월령은 추천
 * 결과를 좌우하는 입력이라, 복제된 계산이 하나만 어긋나면 같은 아이에게 다른 정책이 나온다.
 *
 * <p>그래서 월령 계산을 여기 한 곳에 둔다. 생일이 없으면 {@code null} 이고, {@code null} 을
 * 숫자로 바꿔 주지 않는다 — 0개월로 바꾸면 "갓 태어난 아이" 로 읽혀 신생아 정책이 추천된다.
 *
 * @param childId 자녀 ID
 * @param name 이름. 추천 이유 문장에 들어간다("첫째 아이는 ...")
 * @param birthDate 생년월일. 없을 수 있다
 * @param gender 성별 문자열. 시드·공공데이터가 섞여 들어와 코드값으로 고정하지 않는다
 * @param specialNeeds 특별히 고려할 사항. 비어 있으면 {@code null}
 */
public record ChildView(
        Long childId,
        String name,
        LocalDate birthDate,
        String gender,
        String specialNeeds) {

    public static ChildView from(Child child) {
        return new ChildView(
                child.getId(),
                child.getName(),
                child.getBirthDate(),
                child.getGender(),
                blankToNull(child.getSpecialNeeds()));
    }

    /**
     * 기준일 시점의 월령. 생일이 없으면 {@code null}.
     *
     * <p>기준일을 받는 쪽이 본체다. {@code LocalDate.now()} 를 안에서 부르면 테스트가 "오늘"
     * 에 따라 흔들린다.
     */
    public Integer ageMonths(LocalDate on) {
        if (birthDate == null || on == null) {
            return null;
        }
        return (int) ChronoUnit.MONTHS.between(birthDate, on);
    }

    /** 특별 고려사항이 있는지. 빈 문자열과 {@code null} 을 같게 본다. */
    public boolean hasSpecialNeeds() {
        return specialNeeds != null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
