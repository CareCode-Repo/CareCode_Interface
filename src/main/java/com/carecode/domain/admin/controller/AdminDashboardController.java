package com.carecode.domain.admin.controller;

import com.carecode.domain.health.entity.Hospital;
import com.carecode.domain.health.repository.HospitalRepository;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.repository.PolicyRepository;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 어드민 대시보드 API.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "어드민 - 대시보드", description = "관리자 대시보드 요약 API")
public class AdminDashboardController {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private final UserRepository userRepository;
    private final HospitalRepository hospitalRepository;
    private final PolicyRepository policyRepository;

    @GetMapping("/dashboard")
    @Operation(summary = "대시보드 요약 조회", description = "전체 건수, 최근 활동, 가입자 추이를 반환합니다.")
    public ResponseEntity<Map<String, Object>> dashboard() {
        Map<String, Object> dashboard = new LinkedHashMap<>();

        dashboard.put("userCount", userRepository.count());
        dashboard.put("hospitalCount", hospitalRepository.count());
        dashboard.put("policyCount", policyRepository.count());
        dashboard.put("recentActivities", recentActivities());

        Map<String, Long> userTrend = userTrend();
        dashboard.put("userTrendLabels", new ArrayList<>(userTrend.keySet()));
        dashboard.put("userTrendData", new ArrayList<>(userTrend.values()));

        return ResponseEntity.ok(dashboard);
    }

    private List<Map<String, String>> recentActivities() {
        List<Map<String, String>> activities = new ArrayList<>();

        for (User user : userRepository.findTop2ByDeletedAtIsNullOrderByCreatedAtDesc()) {
            activities.add(Map.of(
                    "type", "user",
                    "desc", "신규 사용자 가입: " + user.getName(),
                    "time", format(user.getCreatedAt())
            ));
        }
        for (Hospital hospital : hospitalRepository.findTop2ByOrderByCreatedAtDesc()) {
            activities.add(Map.of(
                    "type", "hospital",
                    "desc", "병원 등록: " + hospital.getName(),
                    "time", format(hospital.getCreatedAt())
            ));
        }
        for (Policy policy : policyRepository.findTop1ByOrderByCreatedAtDesc()) {
            activities.add(Map.of(
                    "type", "policy",
                    "desc", "정책 등록: " + policy.getTitle(),
                    "time", format(policy.getCreatedAt())
            ));
        }

        return activities.stream()
                .sorted(Comparator.comparing((Map<String, String> m) -> m.get("time")).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }

    /** 최근 6개월 가입자 추이. 값이 없는 달도 0 으로 채워 그래프가 끊기지 않게 한다. */
    private Map<String, Long> userTrend() {
        LocalDate now = LocalDate.now();
        LocalDateTime sixMonthsAgo = now.minusMonths(5).withDayOfMonth(1).atStartOfDay();

        Map<String, Long> lookup = new HashMap<>();
        for (Object[] row : userRepository.countUsersGroupedByMonthSince(sixMonthsAgo)) {
            String key = String.format("%d-%02d", ((Number) row[0]).intValue(), ((Number) row[1]).intValue());
            lookup.put(key, ((Number) row[2]).longValue());
        }

        Map<String, Long> trend = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            String label = now.minusMonths(i).withDayOfMonth(1).format(MONTH);
            trend.put(label, lookup.getOrDefault(label, 0L));
        }
        return trend;
    }

    private String format(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(TIMESTAMP) : "-";
    }
}
