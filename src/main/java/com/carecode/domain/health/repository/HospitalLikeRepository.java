package com.carecode.domain.health.repository;

import com.carecode.domain.health.entity.Hospital;
import com.carecode.domain.health.entity.HospitalLike;
import com.carecode.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HospitalLikeRepository extends JpaRepository<HospitalLike, Long> {
    long countByHospitalId(Long hospitalId);

    /**
     * 내가 찜한 병원 목록.
     *
     * 찜은 걸 수 있는데 모아 볼 방법이 없어 화면을 만들 수 없었다.
     * 병원을 함께 가져오지 않으면 목록 길이만큼 추가 조회가 나간다(N+1).
     */
    @Query("SELECT hl FROM HospitalLike hl JOIN FETCH hl.hospital WHERE hl.userId = :userId ORDER BY hl.createdAt DESC")
    List<HospitalLike> findLikedWithHospitalByUserId(@Param("userId") Long userId);
    boolean existsByHospitalIdAndUserId(Long hospitalId, Long userId);
    void deleteByHospitalIdAndUserId(Long hospitalId, Long userId);
} 