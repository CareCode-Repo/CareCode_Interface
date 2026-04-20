package com.carecode.domain.policy.repository;

import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.entity.PolicyBookmark;
import com.carecode.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PolicyBookmarkRepository extends JpaRepository<PolicyBookmark, Long> {
    Optional<PolicyBookmark> findByUserAndPolicy(User user, Policy policy);
    List<PolicyBookmark> findByUserOrderByCreatedAtDesc(User user);
    void deleteByUserAndPolicy(User user, Policy policy);
}
