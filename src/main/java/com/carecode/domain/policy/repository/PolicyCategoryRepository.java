package com.carecode.domain.policy.repository;

import com.carecode.domain.policy.entity.PolicyCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyCategoryRepository extends JpaRepository<PolicyCategory, Long> {
    
    Optional<PolicyCategory> findByName(String name);
    
    List<PolicyCategory> findByIsActiveTrueOrderByDisplayOrderAsc();
    
    @Query("SELECT pc FROM PolicyCategory pc WHERE pc.name LIKE %:keyword% AND pc.isActive = true")
    List<PolicyCategory> findByNameContainingAndIsActiveTrue(String keyword);
    
    boolean existsByName(String name);
} 