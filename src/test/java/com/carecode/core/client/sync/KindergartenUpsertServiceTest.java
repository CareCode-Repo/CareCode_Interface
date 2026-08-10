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

/** 실제 유치원알리미 basicInfo2 응답을 기준으로 검증한다. */
@DisplayName("유치원 적재")
class KindergartenUpsertServiceTest {

    /** 2026-08-05 실호출로 받은 응답 (옥인유치원). */
    private static final String REAL_ROW = """
            {"key":"1","kindercode":"1ecec08c-f026-b044-e053-0a32095ab044",
             "officeedu":"서울특별시교육청","subofficeedu":"중부교육지원청",
             "kindername":"옥인유치원","establish":"사립(사인)",
             "addr":"서울특별시 종로구 자하문로 69","telno":"02-735-3984",
             "hpaddr":"http://okin.kidis.co.kr","opertime":"08시00분~20시00분",
             "clcnt3":"1","clcnt4":"1","clcnt5":"1","mixclcnt":"0","shclcnt":"0",
             "ppcnt3":"7","ppcnt4":"17","ppcnt5":"17","mixppcnt":"0","shppcnt":"0",
             "prmstfcnt":"90","ag3fpcnt":"15","ag4fpcnt":"20","ag5fpcnt":"22",
             "mixfpcnt":"0","spcnfpcnt":"0","lttdcdnt":"37.5806","lngtcdnt":"126.9662"}
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CareFacilityRepository repository;
    private KindergartenUpsertService service;

    @BeforeEach
    void setUp() {
        repository = mock(CareFacilityRepository.class);
        when(repository.findByFacilityCode(anyString())).thenReturn(Optional.empty());
        service = new KindergartenUpsertService(repository, mock(CapacitySnapshotRecorder.class));
    }

    @Test
    @DisplayName("실제 응답의 기본 정보를 읽는다")
    void mapsRealResponse() {
        boolean isNew = service.upsert(row(REAL_ROW));

        assertThat(isNew).isTrue();
        CareFacility saved = captureSaved();
        assertThat(saved.getName()).isEqualTo("옥인유치원");
        assertThat(saved.getFacilityType()).isEqualTo(FacilityType.KINDERGARTEN);
        assertThat(saved.getPhone()).isEqualTo("02-735-3984");
        assertThat(saved.getOperatingHours()).isEqualTo("08시00분~20시00분");
        assertThat(saved.getLatitude()).isEqualTo(37.5806);
    }

    @Test
    @DisplayName("kindercode 를 시설 코드로 쓴다")
    void usesKinderCodeAsFacilityCode() {
        service.upsert(row(REAL_ROW));

        assertThat(captureSaved().getFacilityCode())
                .isEqualTo(KindergartenUpsertService.CODE_PREFIX + "1ecec08c-f026-b044-e053-0a32095ab044");
    }

    @Test
    @DisplayName("정원은 인가정원이 아니라 연령별 편성정원의 합을 쓴다")
    void usesClassCapacityNotLicensed() {
        service.upsert(row(REAL_ROW));

        CareFacility saved = captureSaved();
        // 인가정원 90 을 쓰면 충원율이 46% 로 실제(72%)보다 낮게 나온다
        assertThat(saved.getCapacity()).isEqualTo(57);
        assertThat(saved.getCurrentEnrollment()).isEqualTo(41);
        assertThat(saved.getAvailableSpots()).isEqualTo(16);
    }

    @Test
    @DisplayName("편성정원이 없으면 인가정원으로 대체한다")
    void fallsBackToLicensedCapacity() {
        service.upsert(row("""
                {"kindercode":"c1","kindername":"테스트유치원","prmstfcnt":"80",
                 "ppcnt3":"10","ppcnt4":"10"}
                """));

        CareFacility saved = captureSaved();
        assertThat(saved.getCapacity()).isEqualTo(80);
        assertThat(saved.getCurrentEnrollment()).isEqualTo(20);
    }

    @Test
    @DisplayName("설립유형으로 국공립을 판별한다")
    void resolvesPublicFromEstablishType() {
        service.upsert(row("{\"kindercode\":\"c1\",\"kindername\":\"가\",\"establish\":\"공립(병설)\"}"));
        assertThat(captureSaved().getIsPublic()).isTrue();

        service.upsert(row("{\"kindercode\":\"c2\",\"kindername\":\"나\",\"establish\":\"사립(사인)\"}"));
        assertThat(captureSaved().getIsPublic()).isFalse();
    }

    @Test
    @DisplayName("주소에서 시도·시군구를 분리한다")
    void splitsRegionFromAddress() {
        service.upsert(row(REAL_ROW));

        CareFacility saved = captureSaved();
        assertThat(saved.getCity()).isEqualTo("서울특별시");
        assertThat(saved.getDistrict()).isEqualTo("종로구");
    }

    @Test
    @DisplayName("null 로 오는 필드가 있어도 합계를 낸다")
    void sumsWithNullFields() {
        // 실제 응답에서 shclcnt·shppcnt 가 null 로 오는 경우가 있다
        service.upsert(row("""
                {"kindercode":"c1","kindername":"가","ag3fpcnt":"15","ag4fpcnt":null,
                 "ppcnt3":"7","ppcnt4":null,"shppcnt":null}
                """));

        CareFacility saved = captureSaved();
        assertThat(saved.getCapacity()).isEqualTo(15);
        assertThat(saved.getCurrentEnrollment()).isEqualTo(7);
    }

    @Test
    @DisplayName("코드나 이름이 없으면 저장하지 않는다")
    void rejectsRowWithoutIdentity() {
        assertThatThrownBy(() -> service.upsert(row("{\"kindername\":\"이름만\"}")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.upsert(row("{\"kindercode\":\"코드만\"}")))
                .isInstanceOf(IllegalArgumentException.class);
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
