package com.carecode.domain.health.mapper;

import com.carecode.core.util.ResponseMapper;
import com.carecode.domain.health.dto.response.HealthResponse;
import com.carecode.domain.health.dto.response.ChildInfoResponse;
import com.carecode.domain.user.entity.Child;
import org.springframework.stereotype.Component;

@Component
public class ChildMapper implements ResponseMapper<Child, ChildInfoResponse> {
    @Override
    public ChildInfoResponse toResponse(Child child) {
        return ChildInfoResponse.builder()
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


