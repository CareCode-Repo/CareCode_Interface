package com.carecode.core.geocoding;

import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.repository.CareFacilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("시설 좌표 보정")
class FacilityGeocodingServiceTest {

    private CareFacilityRepository repository;
    private Geocoder geocoder;
    private FacilityGeocodingService service;

    @BeforeEach
    void setUp() {
        repository = mock(CareFacilityRepository.class);
        geocoder = mock(Geocoder.class);
        when(geocoder.isAvailable()).thenReturn(true);

        service = new FacilityGeocodingService(repository, geocoder);
        ReflectionTestUtils.setField(service, "batchSize", 100);
        // 테스트에서 대기하지 않도록 간격을 0 으로 둔다
        ReflectionTestUtils.setField(service, "delayMs", 0L);
    }

    @Test
    @DisplayName("키가 없으면 조회조차 하지 않는다")
    void skipsWhenGeocoderUnavailable() {
        when(geocoder.isAvailable()).thenReturn(false);

        var result = service.fillMissingCoordinates();

        assertThat(result.getSkippedReason()).contains("키 미설정");
        verify(repository, never()).findMissingCoordinates(any(Pageable.class));
    }

    @Test
    @DisplayName("변환된 좌표를 저장한다")
    void savesResolvedCoordinates() {
        CareFacility facility = facility("서울특별시 종로구 자하문로 69");
        when(repository.findMissingCoordinates(any(Pageable.class))).thenReturn(List.of(facility));
        when(geocoder.geocode(anyString()))
                .thenReturn(Optional.of(new Geocoder.Coordinates(37.5806, 126.9662)));
        when(repository.countMissingCoordinates()).thenReturn(0L);

        var result = service.fillMissingCoordinates();

        assertThat(result.getResolved()).isEqualTo(1);
        assertThat(facility.getLatitude()).isEqualTo(37.5806);
        assertThat(facility.getLongitude()).isEqualTo(126.9662);
        verify(repository).save(facility);
    }

    @Test
    @DisplayName("변환 실패는 세되 다음 건을 계속 처리한다")
    void continuesAfterFailure() {
        List<CareFacility> targets = List.of(facility("주소1"), facility("주소2"), facility("주소3"));
        when(repository.findMissingCoordinates(any(Pageable.class))).thenReturn(targets);
        when(geocoder.geocode("주소1")).thenReturn(Optional.empty());
        when(geocoder.geocode("주소2"))
                .thenReturn(Optional.of(new Geocoder.Coordinates(37.5, 127.0)));
        when(geocoder.geocode("주소3")).thenReturn(Optional.empty());
        when(repository.countMissingCoordinates()).thenReturn(2L);

        var result = service.fillMissingCoordinates();

        assertThat(result.getResolved()).isEqualTo(1);
        assertThat(result.getFailed()).isEqualTo(2);
        assertThat(result.getRemaining()).isEqualTo(2);
    }

    @Test
    @DisplayName("대상이 없으면 조용히 끝낸다")
    void doesNothingWhenNoTargets() {
        when(repository.findMissingCoordinates(any(Pageable.class))).thenReturn(List.of());

        var result = service.fillMissingCoordinates();

        assertThat(result.getResolved()).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("한반도 범위 판정이 동작한다")
    void validatesKoreanBounds() {
        assertThat(new Geocoder.Coordinates(37.5, 127.0).isWithinKorea()).isTrue();
        assertThat(new Geocoder.Coordinates(33.2, 126.5).isWithinKorea()).isTrue();   // 제주
        assertThat(new Geocoder.Coordinates(40.7, -74.0).isWithinKorea()).isFalse();  // 뉴욕
        // 위경도가 뒤집힌 경우
        assertThat(new Geocoder.Coordinates(127.0, 37.5).isWithinKorea()).isFalse();
    }

    private CareFacility facility(String address) {
        return CareFacility.builder().name("테스트시설").address(address).isActive(true).build();
    }
}
