package com.carecode.domain.user.repository;

import com.carecode.domain.user.entity.ConsentType;
import com.carecode.domain.user.entity.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserConsentRepository extends JpaRepository<UserConsent, Long> {

    List<UserConsent> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 항목별 가장 최근 동의 이력. 이력은 append-only 라서 현재 동의 상태는 최신 행으로 판단한다. */
    @Query("SELECT uc FROM UserConsent uc " +
           "WHERE uc.user.id = :userId AND uc.consentType = :consentType " +
           "ORDER BY uc.createdAt DESC LIMIT 1")
    Optional<UserConsent> findLatest(@Param("userId") Long userId,
                                     @Param("consentType") ConsentType consentType);
}
