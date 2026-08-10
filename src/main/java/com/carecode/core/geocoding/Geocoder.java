package com.carecode.core.geocoding;

import java.util.Optional;

/** 주소 → 좌표 변환. 공급자를 바꿔도 호출부가 흔들리지 않도록 분리한다. */
public interface Geocoder {

    /** 좌표 한 쌍. */
    record Coordinates(double latitude, double longitude) {

        /** 한반도 범위를 벗어나면 잘못 변환된 값이다. */
        public boolean isWithinKorea() {
            return latitude >= 33.0 && latitude <= 39.5
                    && longitude >= 124.0 && longitude <= 132.0;
        }
    }

    String getProviderName();

    /** 키가 없으면 비활성 상태로 두고 기능만 건너뛴다. */
    boolean isAvailable();

    /** 변환에 실패하면 비어 있는 값을 돌려준다. 예외로 배치를 중단시키지 않는다. */
    Optional<Coordinates> geocode(String address);
}
