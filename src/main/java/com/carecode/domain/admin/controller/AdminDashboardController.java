package com.carecode.domain.admin.controller;

import com.carecode.domain.user.repository.UserRepository;
import com.carecode.domain.health.repository.HospitalRepository;
import com.carecode.domain.policy.repository.PolicyRepository;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.health.entity.Hospital;
import com.carecode.domain.policy.entity.Policy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import com.carecode.core.conponents.JsonMapComponent;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {
    private final UserRepository userRepository;
    private final HospitalRepository hospitalRepository;
    private final PolicyRepository policyRepository;

    @GetMapping("/login")
    public String adminLogin(Model model) {
        return "admin/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        JsonMapComponent dashboardData = new JsonMapComponent();

        // 1. 전체 수
        long userCount = userRepository.count();
        long hospitalCount = hospitalRepository.count();
        long policyCount = policyRepository.count();
        dashboardData.put("userCount", userCount);
        dashboardData.put("hospitalCount", hospitalCount);
        dashboardData.put("policyCount", policyCount);

        // 2. 최근 활동 (최신순 5개)
        List<Map<String, String>> recentActivities = new ArrayList<>();
        List<User> allUsers = userRepository.findAll();
        allUsers.sort(Comparator.comparing(User::getCreatedAt).reversed());
        for (int i = 0; i < Math.min(2, allUsers.size()); i++) {
            User user = allUsers.get(i);
            recentActivities.add(Map.of(
                "type", "user",
                "desc", "신규 사용자 가입: " + user.getName(),
                "time", user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            ));
        }
        List<Hospital> allHospitals = hospitalRepository.findAll();
        allHospitals.sort(Comparator.comparing(Hospital::getCreatedAt).reversed());
        for (int i = 0; i < Math.min(2, allHospitals.size()); i++) {
            Hospital hospital = allHospitals.get(i);
            recentActivities.add(Map.of(
                "type", "hospital",
                "desc", "병원 등록: " + hospital.getName(),
                "time", hospital.getCreatedAt() != null ? hospital.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "-"
            ));
        }
        List<Policy> allPolicies = policyRepository.findAll();
        allPolicies.sort(Comparator.comparing(Policy::getCreatedAt).reversed());
        for (int i = 0; i < Math.min(1, allPolicies.size()); i++) {
            Policy policy = allPolicies.get(i);
            recentActivities.add(Map.of(
                "type", "policy",
                "desc", "정책 등록: " + policy.getTitle(),
                "time", policy.getCreatedAt() != null ? policy.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "-"
            ));
        }
        // 최신순 정렬
        recentActivities = recentActivities.stream()
                .sorted(Comparator.comparing(m -> m.get("time"), Comparator.reverseOrder()))
                .limit(5)
                .collect(Collectors.toList());
        dashboardData.put("recentActivities", recentActivities);

        // 3. 가입자 추이 (최근 6개월)
        Map<String, Long> userTrend = new LinkedHashMap<>();
        LocalDate now = LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            LocalDate monthVal = now.minusMonths(i).withDayOfMonth(1);
            String labelVal = monthVal.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            long count = userRepository.findAll().stream()
                    .filter(u -> u.getCreatedAt() != null &&
                            u.getCreatedAt().getYear() == monthVal.getYear() &&
                            u.getCreatedAt().getMonthValue() == monthVal.getMonthValue())
                    .count();
            userTrend.put(labelVal, count);
        }
        dashboardData.put("userTrendLabels", new ArrayList<>(userTrend.keySet()));
        dashboardData.put("userTrendData", new ArrayList<>(userTrend.values()));

        model.addAttribute("dashboard", dashboardData);
        return "admin/dashboard";
    }
} 