package com.carecode.domain.user.app;

import java.util.List;

/**
 * 자녀 정보를 다른 도메인에 내주는 입구.
 *
 * <p>자녀는 {@code user} 도메인에 속한다(계정에 딸린 정보이고, 계정이 사라지면 함께 익명화된다).
 * 그런데 자녀를 쓰는 쪽은 health·policy·facility 다. 지금까지는 그 도메인들이
 * {@code ChildRepository} 를 그냥 주입해 썼다. 그렇게 하면 두 가지가 생긴다.
 *
 * <ul>
 *   <li><b>소유권 검증이 흩어진다.</b> "이 아이가 이 사용자의 아이인가" 를 세 군데가 각자
 *       구현하고 있었다. 보안 검증은 복제되면 한 쪽이 조용히 뒤처진다.</li>
 *   <li><b>같은 계산이 복제된다.</b> 월령 계산이 다섯 군데에 있었고 생일이 없을 때의 처리가
 *       곳마다 달랐다. {@link ChildView#ageMonths} 참고.</li>
 * </ul>
 *
 * <p>그래서 읽기는 이 입구로만 받는다. 엔티티가 아니라 {@link ChildView} 를 돌려주므로,
 * 쓰는 쪽이 실수로 자녀 정보를 고치거나 연관을 타고 사용자 정보까지 들어가는 일이 없다.
 *
 * <p><b>여기 없는 것</b>: 자녀 등록·수정·삭제. 그건 {@code user} 도메인 안에서만 한다.
 * 또 자녀 엔티티 자체가 필요한 경우(건강 기록을 자녀에 붙이는 등)는 {@code health} 가
 * 같은 도메인 안에서 다루므로 이 입구를 쓰지 않는다.
 */
public interface ChildDirectory {

    /**
     * 한 사용자의 자녀 목록. 최근에 등록한 순이다.
     *
     * <p>등록된 자녀가 없으면 빈 목록이다. 호출하는 쪽은 그 경우를 "0개월 아이" 로 바꾸지
     * 말고 추천을 내지 않는 쪽으로 처리해야 한다.
     */
    List<ChildView> childrenOf(Long userId);

    /**
     * 그 사용자의 자녀인지 확인하고 돌려준다.
     *
     * <p>없는 자녀와 남의 자녀를 <b>구분하지 않고</b> 둘 다 "찾을 수 없다" 로 끝낸다.
     * 403 과 404 를 나눠 주면 ID 를 훑어 "이 번호는 누군가의 자녀" 라는 사실을 알아낼 수 있다.
     *
     * @throws com.carecode.core.exception.ChildNotFoundException 없거나 다른 사용자의 자녀일 때
     */
    ChildView requireOwnedChild(Long childId, Long parentUserId);
}
