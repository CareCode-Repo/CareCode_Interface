package com.carecode.core.util;

/** 반경 검색용 위경도 사각 범위. 인덱스를 탈 수 있는 BETWEEN 조건으로 후보를 좁히는 데 쓴다. */
public record BoundingBox(double minLat, double maxLat, double minLng, double maxLng) {

    /** 위도 1도당 거리(km). 경도와 달리 위치에 관계없이 거의 일정하다. */
    private static final double KM_PER_LAT_DEGREE = 111.045;

    /** 중심점과 반경(km)으로 사각 범위를 만든다. */
    public static BoundingBox around(double latitude, double longitude, double radiusKm) {
        double latDelta = radiusKm / KM_PER_LAT_DEGREE;

        // 경도 1도의 거리는 위도가 높을수록 짧아진다. 극지방에서 0으로 나누지 않도록 하한을 둔다.
        double cosLat = Math.max(Math.cos(Math.toRadians(latitude)), 0.01);
        double lngDelta = radiusKm / (KM_PER_LAT_DEGREE * cosLat);

        return new BoundingBox(
                clampLat(latitude - latDelta),
                clampLat(latitude + latDelta),
                longitude - lngDelta,
                longitude + lngDelta);
    }

    private static double clampLat(double lat) {
        return Math.max(-90.0, Math.min(90.0, lat));
    }
}
