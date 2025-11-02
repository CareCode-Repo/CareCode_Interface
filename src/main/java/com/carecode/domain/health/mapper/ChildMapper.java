package com.carecode.domain.health.mapper;

import com.carecode.core.util.ResponseMapper;
import com.carecode.domain.health.dto.HealthResponse;
import com.carecode.domain.user.entity.Child;
import org.springframework.stereotype.Component;

@Component
public class ChildMapper implements ResponseMapper<Child, HealthResponse.Child> {
    @Override
    public HealthResponse.Child toResponse(Child child) {
        return HealthResponse.Child.builder()
                .id(child.getId())
                .userId(child.getUser().getId())
                .name(child.getName())
                .birthDate(child.getBirthDate().toString())
                .gender(child.getGender())
                .createdAt(child.getCreatedAt().toString())
                .updatedAt(child.getUpdatedAt() != null ? child.getUpdatedAt().toString() : null)
                .build();
    }
}


