package com.carecode.domain.community.repository;

import com.carecode.domain.community.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    
    Optional<Tag> findByName(String name);
    
    List<Tag> findByIsActiveTrue();
    
    @Query("SELECT t FROM Tag t WHERE t.name LIKE %:keyword% AND t.isActive = true")
    List<Tag> findByNameContainingAndIsActiveTrue(@Param("keyword") String keyword);
    
    boolean existsByName(String name);
} 