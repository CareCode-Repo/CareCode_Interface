package com.carecode.core.client.sync;

import com.carecode.domain.careFacility.entity.CareFacility;
import com.carecode.domain.careFacility.entity.FacilityType;
import com.carecode.domain.careFacility.repository.CareFacilityRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("유치원 적재")
class KindergartenUpsertServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CareFacilityRepository repository;
    private KindergartenUpsertService service;

    @BeforeEach
    void setUp() {
        repository = mock(CareFacilityRepository.class);
        when(repository.findByFacilityCode(anyString())).thenReturn(Optional.empty());
        service = new KindergartenUpsertService(repository);
    }

    @Test
    @DisplayName("표준데이터 한글 필드명을 읽는다")
    void mapsStandardDataFields() {
        boolean isNew = service.upsert(row("""
                {"유치원명":"행복유치원","소재지도로명주소":"서울특별시 강남구 테헤란로 1",
                 "전화번호":"02-123-4567","설립유형":"공립","정원":"100","현원":"80",
                 "위도":"37.5","경도":"127.0","시도명":"서울특별시","시군구명":"강남구"}
                """));

        assertThat(isNew).isTrue();
        CareFacility saved = captureSaved();
        assertThat(saved.getName()).isEqualTo("행복유치원");
        assertThat(saved.getFacilityType()).isEqualTo(FacilityType.KINDERGARTEN);
        assertThat(saved.getIsPublic()).isTrue();
        assertThat(saved.getAvailableSpots()).isEqualTo(20);
        assertThat(saved.getLatitude()).isEqualTo(37.5);
        assertThat(saved.getCity()).isEqualTo("서울특별시");
    }

    @Test
    @DisplayName("영문 필드명으로 와도 동일하게 읽는다")
    void mapsEnglishFieldNames() {
        service.upsert(row("""
                {"kindrgrtnNm":"한빛유치원","rdnmadr":"부산광역시 해운대구 1","telno":"051-1234-5678"}
                """));

        CareFacility saved = captureSaved();
        assertThat(saved.getName()).isEqualTo("한빛유치원");
        assertThat(saved.getPhone()).isEqualTo("051-1234-5678");
    }

    @Test
    @DisplayName("고유 코드가 없어도 같은 유치원은 같은 코드가 나온다")
    void generatesStableCodeFromNaturalKey() {
        String json = """
                {"유치원명":"행복유치원","소재지도로명주소":"서울특별시 강남구 테헤란로 1"}
                """;

        service.upsert(row(json));
        String first = captureSaved().getFacilityCode();

        service.upsert(row(json));
        String second = captureSaved().getFacilityCode();

        assertThat(first).isEqualTo(second).startsWith(KindergartenUpsertService.CODE_PREFIX);
    }

    @Test
    @DisplayName("이름이 같아도 주소가 다르면 다른 시설로 본다")
    void distinguishesSameNameDifferentAddress() {
        service.upsert(row("{\"유치원명\":\"행복유치원\",\"소재지도로명주소\":\"서울특별시 강남구 1\"}"));
        String seoul = captureSaved().getFacilityCode();

        service.upsert(row("{\"유치원명\":\"행복유치원\",\"소재지도로명주소\":\"부산광역시 해운대구 1\"}"));
        String busan = captureSaved().getFacilityCode();

        assertThat(seoul).isNotEqualTo(busan);
    }

    @Test
    @DisplayName("고유 코드가 있으면 그것을 쓴다")
    void prefersExternalCode() {
        service.upsert(row("{\"유치원명\":\"행복유치원\",\"유치원코드\":\"K12345\"}"));

        assertThat(captureSaved().getFacilityCode())
                .isEqualTo(KindergartenUpsertService.CODE_PREFIX + "K12345");
    }

    @Test
    @DisplayName("유치원명이 없으면 저장하지 않는다")
    void rejectsRowWithoutName() {
        assertThatThrownBy(() -> service.upsert(row("{\"소재지도로명주소\":\"서울특별시\"}")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유치원명");
    }

    private CareFacility captureSaved() {
        ArgumentCaptor<CareFacility> captor = ArgumentCaptor.forClass(CareFacility.class);
        verify(repository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    private JsonNode row(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
