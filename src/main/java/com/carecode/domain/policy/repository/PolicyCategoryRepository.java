package com.carecode.domain.policy.repository;

import com.carecode.domain.policy.entity.PolicyCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyCategoryRepository extends JpaRepository<PolicyCategory, Long> {
    
    List<PolicyCategory> findByIsActiveTrueOrderByDisplayOrderAsc();

    boolean existsByName(String name);
} 