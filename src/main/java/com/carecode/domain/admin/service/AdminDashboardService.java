package com.carecode.domain.admin.service;

import com.carecode.domain.admin.dto.response.AdminDashboardResponse;
import com.carecode.domain.admin.dto.response.AdminDashboardResponse.RecentActivity;
import com.carecode.domain.health.entity.Hospital;
import com.carecode.domain.health.repository.HospitalRepository;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.repository.PolicyRepository;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 어드민 대시보드 요약.
 *
 * <p>세 도메인(user·health·policy)의 리포지토리를 함께 읽는다. 어드민은 본래 전 도메인을
 * 관리하는 화면이라 이 조합 자체는 피할 수 없지만, 조합을 컨트롤러에 두면 화면이 바뀔 때마다
 * 웹 계층을 고치게 된다. 숫자를 세는 일은 여기에 둔다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private final UserRepository userRepository;
    private final HospitalRepository hospitalRepository;
    private final PolicyRepository policyRepository;

    public AdminDashboardResponse summary() {
        Map<String, Long> userTrend = userTrend();

        return new AdminDashboardResponse(
                userRepository.count(),
                hospitalRepository.count(),
                policyRepository.count(),
                recentActivities(),
                new ArrayList<>(userTrend.keySet()),
                new ArrayList<>(userTrend.values()));
    }

    private List<RecentActivity> recentActivities() {
        List<RecentActivity> activities = new ArrayList<>();

        for (User user : userRepository.findTop2ByDeletedAtIsNullOrderByCreatedAtDesc()) {
            activities.add(new RecentActivity("user", "신규 사용자 가입: " + user.getName(), format(user.getCreatedAt())));
        }
        for (Hospital hospital : hospitalRepository.findTop2ByOrderByCreatedAtDesc()) {
            activities.add(new RecentActivity("hospital", "병원 등록: " + hospital.getName(), format(hospital.getCreatedAt())));
        }
        for (Policy policy : policyRepository.findTop1ByOrderByCreatedAtDesc()) {
            activities.add(new RecentActivity("policy", "정책 등록: " + policy.getTitle(), format(policy.getCreatedAt())));
        }

        return activities.stream()
                .sorted(Comparator.comparing(RecentActivity::time).reversed())
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
