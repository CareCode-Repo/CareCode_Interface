package com.carecode.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("바운딩 박스 계산")
class BoundingBoxTest {

    @Test
    @DisplayName("반경만큼 위도 범위를 넓힌다")
    void expandsLatitudeByRadius() {
        BoundingBox box = BoundingBox.around(37.5665, 126.9780, 5);

        // 위도 1도 ≈ 111km 이므로 5km 는 약 0.045도
        assertThat(box.maxLat() - box.minLat()).isCloseTo(0.09, org.assertj.core.data.Offset.offset(0.005));
        assertThat(box.minLat()).isLessThan(37.5665);
        assertThat(box.maxLat()).isGreaterThan(37.5665);
    }

    @Test
    @DisplayName("고위도일수록 경도 범위가 넓어진다")
    void widensLongitudeAtHighLatitude() {
        BoundingBox seoul = BoundingBox.around(37.5, 127.0, 5);
        BoundingBox equator = BoundingBox.around(0.0, 127.0, 5);

        double seoulWidth = seoul.maxLng() - seoul.minLng();
        double equatorWidth = equator.maxLng() - equator.minLng();
        assertThat(seoulWidth).isGreaterThan(equatorWidth);
    }

    @Test
    @DisplayName("극지방에서도 위도가 범위를 벗어나지 않는다")
    void clampsLatitudeAtPoles() {
        BoundingBox box = BoundingBox.around(89.9, 0.0, 100);

        assertThat(box.maxLat()).isLessThanOrEqualTo(90.0);
        assertThat(box.minLat()).isGreaterThanOrEqualTo(-90.0);
    }
}
