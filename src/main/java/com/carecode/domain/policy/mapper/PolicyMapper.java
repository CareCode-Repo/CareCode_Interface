package com.carecode.domain.policy.mapper;

import com.carecode.core.util.ResponseMapper;
import com.carecode.domain.policy.dto.PolicyDto;
import com.carecode.domain.policy.entity.Policy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class PolicyMapper implements ResponseMapper<Policy, PolicyDto> {

    @Override
    public PolicyDto toResponse(Policy policy) {
        return PolicyDto.builder()
                .id(policy.getId())
                .title(policy.getTitle())
                .description(policy.getDescription())
                .category(policy.getPolicyCategory() != null ? policy.getPolicyCategory().getName() : policy.getPolicyType())
                .location(policy.getTargetRegion())
                .minAge(policy.getTargetAgeMin())
                .maxAge(policy.getTargetAgeMax())
                .supportAmount(policy.getBenefitAmount())
                .applicationPeriod(formatApplicationPeriod(policy.getApplicationStartDate(), policy.getApplicationEndDate()))
                .eligibilityCriteria(null)
                .applicationMethod(policy.getApplicationUrl())
                .requiredDocuments(policy.getRequiredDocuments())
                .contactInfo(policy.getContactInfo())
                .websiteUrl(policy.getApplicationUrl())
                .viewCount(0)
                .isActive(policy.getIsActive())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }

    private String formatApplicationPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) return "상시 신청";
        if (startDate == null) return "~ " + endDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        if (endDate == null) return startDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")) + " ~";
        return startDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")) + " ~ " + endDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
    }

    private String formatWebsiteUrl(String applicationUrl) { return applicationUrl; }
}


