package com.carecode.domain.health.repository;

import com.carecode.domain.health.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {
    List<Hospital> findByType(String type);

    @Query("SELECT h FROM Hospital h WHERE FUNCTION('ST_Distance_Sphere', point(h.longitude, h.latitude), point(:lng, :lat)) <= :radius")
    List<Hospital> findNearby(@Param("lat") double lat, @Param("lng") double lng, @Param("radius") double radius);
} 