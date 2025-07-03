package com.carecode.core.util;

import lombok.extern.slf4j.Slf4j;

/**
 * 위치 관련 유틸리티 클래스
 * 육아 서비스에서 위치 기반 검색 및 거리 계산에 활용
 */
@Slf4j
public class LocationUtil {

    private static final double EARTH_RADIUS = 6371; // 지구 반지름 (km)

    /**
     * 두 지점 간의 거리를 계산 (Haversine 공식)
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS * c;
    }

    /**
     * 주어진 반경 내에 있는지 확인
     */
    public static boolean isWithinRadius(double centerLat, double centerLon, 
                                       double targetLat, double targetLon, double radiusKm) {
        double distance = calculateDistance(centerLat, centerLon, targetLat, targetLon);
        return distance <= radiusKm;
    }

    /**
     * 위도/경도 유효성 검사
     */
    public static boolean isValidCoordinates(double latitude, double longitude) {
        return latitude >= -90 && latitude <= 90 && 
               longitude >= -180 && longitude <= 180;
    }

    /**
     * 지역 코드를 시/도로 변환
     */
    public static String getRegionFromCode(String regionCode) {
        if (regionCode == null || regionCode.length() < 2) {
            return "알 수 없음";
        }
        
        String code = regionCode.substring(0, 2);
        switch (code) {
            case "11": return "서울특별시";
            case "26": return "부산광역시";
            case "27": return "대구광역시";
            case "28": return "인천광역시";
            case "29": return "광주광역시";
            case "30": return "대전광역시";
            case "31": return "울산광역시";
            case "36": return "세종특별자치시";
            case "41": return "경기도";
            case "42": return "강원도";
            case "43": return "충청북도";
            case "44": return "충청남도";
            case "45": return "전라북도";
            case "46": return "전라남도";
            case "47": return "경상북도";
            case "48": return "경상남도";
            case "50": return "제주특별자치도";
            default: return "알 수 없음";
        }
    }
} 