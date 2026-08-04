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

    @Query("""
           SELECT h
           FROM Hospital h
           LEFT JOIN HospitalLike hl ON hl.hospital.id = h.id
           GROUP BY h
           ORDER BY COUNT(hl.id) DESC
           """)
    List<Hospital> findPopularHospitals(Pageable pageable);
} 