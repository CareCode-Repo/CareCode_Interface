package com.carecode.domain.facility.repository;

import com.carecode.domain.facility.entity.ForecastAccuracy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ForecastAccuracyRepository extends JpaRepository<ForecastAccuracy, Long> {

    Optional<ForecastAccuracy> findFirstByHorizonMonthsOrderByRunDateDesc(int horizonMonths);

    List<ForecastAccuracy> findByRunDateOrderByHorizonMonthsAsc(java.time.LocalDate runDate);

    Optional<ForecastAccuracy> findFirstByOrderByRunDateDesc();
}
