package com.carecode.domain.careFacility.service;

import com.carecode.core.exception.CareFacilityNotFoundException;
import com.carecode.domain.careFacility.dto.CareFacilityDto;
import com.carecode.domain.careFacility.dto.CareFacilitySearchRequestDto;
import com.carecode.domain.careFacility.dto.CareFacilitySearchResponseDto;
import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.repository.CareFacilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CareFacilityServiceTest {

    @Mock
    private CareFacilityRepository careFacilityRepository;

    @InjectMocks
    private CareFacilityService careFacilityService;

    private CareFacility testFacility;
    private CareFacilityDto testFacilityDto;

    @BeforeEach
    void setUp() {
        testFacility = CareFacility.builder()
                .id(1L)
                .name("테스트 어린이집")
                .facilityType("DAYCARE")
                .description("테스트 어린이집 설명")
                .address("서울시 강남구 테스트로 123")
                .location("서울시 강남구")
                .latitude(37.5665)
                .longitude(126.9780)
                .phoneNumber("02-1234-5678")
                .email("test@daycare.kr")
                .websiteUrl("https://test-daycare.kr")
                .operatingHours("07:00-19:00")
                .capacity(50)
                .currentEnrollment(30)
                .minAge(3)
                .maxAge(6)
                .rating(4.5)
                .reviewCount(20)
                .viewCount(150)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testFacilityDto = CareFacilityDto.builder()
                .id(1L)
                .name("테스트 어린이집")
                .facilityType("DAYCARE")
                .description("테스트 어린이집 설명")
                .address("서울시 강남구 테스트로 123")
                .location("서울시 강남구")
                .latitude(37.5665)
                .longitude(126.9780)
                .phoneNumber("02-1234-5678")
                .email("test@daycare.kr")
                .websiteUrl("https://test-daycare.kr")
                .operatingHours("07:00-19:00")
                .capacity(50)
                .currentEnrollment(30)
                .minAge(3)
                .maxAge(6)
                .rating(4.5)
                .reviewCount(20)
                .viewCount(150)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("전체 돌봄 시설 목록 조회 성공")
    void getAllCareFacilities_Success() {
        // given
        List<CareFacility> facilities = List.of(testFacility);
        when(careFacilityRepository.findAll()).thenReturn(facilities);

        // when
        List<CareFacilityDto> result = careFacilityService.getAllCareFacilities();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("테스트 어린이집");
        verify(careFacilityRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("돌봄 시설 상세 조회 성공")
    void getCareFacilityById_Success() {
        // given
        when(careFacilityRepository.findById(1L)).thenReturn(Optional.of(testFacility));

        // when
        CareFacilityDto result = careFacilityService.getCareFacilityById(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("테스트 어린이집");
        assertThat(result.getFacilityType()).isEqualTo("DAYCARE");
        verify(careFacilityRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("돌봄 시설 상세 조회 실패 - 존재하지 않는 시설")
    void getCareFacilityById_NotFound() {
        // given
        when(careFacilityRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> careFacilityService.getCareFacilityById(999L))
                .isInstanceOf(CareFacilityNotFoundException.class)
                .hasMessage("돌봄 시설을 찾을 수 없습니다: 999");
        verify(careFacilityRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("돌봄 시설 검색 성공")
    void searchCareFacilities_Success() {
        // given
        CareFacilitySearchRequestDto request = CareFacilitySearchRequestDto.builder()
                .keyword("어린이집")
                .facilityType("DAYCARE")
                .location("서울시 강남구")
                .latitude(37.5665)
                .longitude(126.9780)
                .radius(5.0)
                .page(0)
                .size(10)
                .build();

        Page<CareFacility> facilityPage = new PageImpl<>(List.of(testFacility), PageRequest.of(0, 10), 1);
        when(careFacilityRepository.findBySearchCriteria(
                eq("어린이집"), eq("DAYCARE"), eq("서울시 강남구"), 
                eq(37.5665), eq(126.9780), eq(5.0), any(Pageable.class)
        )).thenReturn(facilityPage);

        // when
        CareFacilitySearchResponseDto result = careFacilityService.searchCareFacilities(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getFacilities()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getCurrentPage()).isEqualTo(0);
        verify(careFacilityRepository, times(1)).findBySearchCriteria(
                eq("어린이집"), eq("DAYCARE"), eq("서울시 강남구"), 
                eq(37.5665), eq(126.9780), eq(5.0), any(Pageable.class)
        );
    }

    @Test
    @DisplayName("시설 유형별 조회 성공")
    void getCareFacilitiesByType_Success() {
        // given
        List<CareFacility> facilities = List.of(testFacility);
        when(careFacilityRepository.findByFacilityType("DAYCARE")).thenReturn(facilities);

        // when
        List<CareFacilityDto> result = careFacilityService.getCareFacilitiesByType("DAYCARE");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFacilityType()).isEqualTo("DAYCARE");
        verify(careFacilityRepository, times(1)).findByFacilityType("DAYCARE");
    }

    @Test
    @DisplayName("지역별 돌봄 시설 조회 성공")
    void getCareFacilitiesByLocation_Success() {
        // given
        List<CareFacility> facilities = List.of(testFacility);
        when(careFacilityRepository.findByLocation("서울시 강남구")).thenReturn(facilities);

        // when
        List<CareFacilityDto> result = careFacilityService.getCareFacilitiesByLocation("서울시 강남구");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLocation()).isEqualTo("서울시 강남구");
        verify(careFacilityRepository, times(1)).findByLocation("서울시 강남구");
    }

    @Test
    @DisplayName("반경 내 돌봄 시설 조회 성공")
    void getCareFacilitiesWithinRadius_Success() {
        // given
        List<CareFacility> facilities = List.of(testFacility);
        when(careFacilityRepository.findWithinRadius(37.5665, 126.9780, 5.0)).thenReturn(facilities);

        // when
        List<CareFacilityDto> result = careFacilityService.getCareFacilitiesWithinRadius(37.5665, 126.9780, 5.0);

        // then
        assertThat(result).hasSize(1);
        verify(careFacilityRepository, times(1)).findWithinRadius(37.5665, 126.9780, 5.0);
    }

    @Test
    @DisplayName("연령대별 돌봄 시설 조회 성공")
    void getCareFacilitiesByAgeRange_Success() {
        // given
        List<CareFacility> facilities = List.of(testFacility);
        when(careFacilityRepository.findByAgeRange(3, 6)).thenReturn(facilities);

        // when
        List<CareFacilityDto> result = careFacilityService.getCareFacilitiesByAgeRange(3, 6);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMinAge()).isEqualTo(3);
        assertThat(result.get(0).getMaxAge()).isEqualTo(6);
        verify(careFacilityRepository, times(1)).findByAgeRange(3, 6);
    }

    @Test
    @DisplayName("운영 시간별 돌봄 시설 조회 성공")
    void getCareFacilitiesByOperatingHours_Success() {
        // given
        List<CareFacility> facilities = List.of(testFacility);
        when(careFacilityRepository.findByOperatingHours("07:00-19:00")).thenReturn(facilities);

        // when
        List<CareFacilityDto> result = careFacilityService.getCareFacilitiesByOperatingHours("07:00-19:00");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOperatingHours()).isEqualTo("07:00-19:00");
        verify(careFacilityRepository, times(1)).findByOperatingHours("07:00-19:00");
    }

    @Test
    @DisplayName("인기 돌봄 시설 조회 성공")
    void getPopularCareFacilities_Success() {
        // given
        List<CareFacility> facilities = List.of(testFacility);
        when(careFacilityRepository.findPopularFacilities(10)).thenReturn(facilities);

        // when
        List<CareFacilityDto> result = careFacilityService.getPopularCareFacilities(10);

        // then
        assertThat(result).hasSize(1);
        verify(careFacilityRepository, times(1)).findPopularFacilities(10);
    }

    @Test
    @DisplayName("신규 돌봄 시설 조회 성공")
    void getNewCareFacilities_Success() {
        // given
        List<CareFacility> facilities = List.of(testFacility);
        when(careFacilityRepository.findNewFacilities(10)).thenReturn(facilities);

        // when
        List<CareFacilityDto> result = careFacilityService.getNewCareFacilities(10);

        // then
        assertThat(result).hasSize(1);
        verify(careFacilityRepository, times(1)).findNewFacilities(10);
    }

    @Test
    @DisplayName("돌봄 시설 조회수 증가 성공")
    void incrementViewCount_Success() {
        // given
        when(careFacilityRepository.findById(1L)).thenReturn(Optional.of(testFacility));
        when(careFacilityRepository.save(any(CareFacility.class))).thenReturn(testFacility);

        // when
        careFacilityService.incrementViewCount(1L);

        // then
        verify(careFacilityRepository, times(1)).findById(1L);
        verify(careFacilityRepository, times(1)).save(any(CareFacility.class));
    }

    @Test
    @DisplayName("돌봄 시설 평점 업데이트 성공")
    void updateRating_Success() {
        // given
        when(careFacilityRepository.findById(1L)).thenReturn(Optional.of(testFacility));
        when(careFacilityRepository.save(any(CareFacility.class))).thenReturn(testFacility);

        // when
        careFacilityService.updateRating(1L, 4.8);

        // then
        verify(careFacilityRepository, times(1)).findById(1L);
        verify(careFacilityRepository, times(1)).save(any(CareFacility.class));
    }

    @Test
    @DisplayName("돌봄 시설 통계 조회 성공")
    void getFacilityStats_Success() {
        // given
        when(careFacilityRepository.count()).thenReturn(100L);
        when(careFacilityRepository.getTotalViewCount()).thenReturn(1000L);
        when(careFacilityRepository.getTypeStats()).thenReturn(List.of());

        // when
        CareFacilitySearchResponseDto.FacilityStats result = careFacilityService.getFacilityStats();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTotalFacilities()).isEqualTo(100L);
        assertThat(result.getTotalViews()).isEqualTo(1000L);
        verify(careFacilityRepository, times(1)).count();
        verify(careFacilityRepository, times(1)).getTotalViewCount();
        verify(careFacilityRepository, times(1)).getTypeStats();
    }
} 