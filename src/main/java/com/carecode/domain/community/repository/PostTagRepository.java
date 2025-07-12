package com.carecode.domain.community.repository;

import com.carecode.domain.community.entity.PostTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostTagRepository extends JpaRepository<PostTag, Long> {
    
    List<PostTag> findByPostId(Long postId);
    
    @Query("SELECT pt FROM PostTag pt JOIN FETCH pt.tag WHERE pt.post.id = :postId")
    List<PostTag> findByPostIdWithTag(@Param("postId") Long postId);
    
    @Query("SELECT pt FROM PostTag pt JOIN FETCH pt.post WHERE pt.tag.id = :tagId")
    List<PostTag> findByTagIdWithPost(@Param("tagId") Long tagId);
    
    void deleteByPostId(Long postId);
    
    boolean existsByPostIdAndTagId(Long postId, Long tagId);
} 