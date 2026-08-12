package com.carecode.domain.health.repository;

import com.carecode.domain.health.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {
    List<Hospital> findByType(String type);

    /** 공공데이터 동기화 시 중복 적재 방지용 조회. */
    Optional<Hospital> findByExternalCode(String externalCode);

    @Query("SELECT h FROM Hospital h WHERE FUNCTION('ST_Distance_Sphere', point(h.longitude, h.latitude), point(:lng, :lat)) <= :radius")
    List<Hospital> findNearby(@Param("lat") double lat, @Param("lng") double lng, @Param("radius") double radius);

    List<Hospital> findTop2ByOrderByCreatedAtDesc();

    /**
     * 진료과목별 등록 수.
     *
     * 소개 사이트가 "소아청소년과 N곳" 을 보여 주는데 그 수를 얻을 공개 경로가 없었다.
     * 목록을 받아 세면 페이지 상한에 걸려 실제보다 적게 나온다.
     */
    @Query("SELECT h.type AS type, COUNT(h) AS count FROM Hospital h WHERE h.type IS NOT NULL GROUP BY h.type")
    List<TypeCount> countByType();

    /** Object[] 로 받으면 캐스팅이 흩어져 컴파일러가 잡아 주지 못한다. */
    interface TypeCount {
        String getType();

        long getCount();
    }

    @Query("""
           SELECT h
           FROM Hospital h
           LEFT JOIN HospitalLike hl ON hl.hospital.id = h.id
           GROUP BY h
           ORDER BY COUNT(hl.id) DESC
           """)
    List<Hospital> findPopularHospitals(Pageable pageable);
} 