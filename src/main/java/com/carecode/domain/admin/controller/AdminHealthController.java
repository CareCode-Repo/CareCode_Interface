package com.carecode.domain.admin.controller;

import com.carecode.domain.health.entity.HealthRecord;
import com.carecode.domain.health.repository.HealthRecordRepository;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/health")
@RequiredArgsConstructor
public class AdminHealthController {
    private final HealthRecordRepository healthRecordRepository;
    private final UserRepository userRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("records", healthRecordRepository.findAll());
        return "admin/health/list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("record", new HealthRecord());
        model.addAttribute("users", userRepository.findAll());
        return "admin/health/form";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute HealthRecord record, @RequestParam(required = false) Long userId) {
        if (userId != null) {
            userRepository.findById(userId).ifPresent(record::setUser);
        }
        healthRecordRepository.save(record);
        return "redirect:/admin/health";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("record", healthRecordRepository.findById(id).orElse(null));
        return "admin/health/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("record", healthRecordRepository.findById(id).orElse(null));
        model.addAttribute("users", userRepository.findAll());
        return "admin/health/form";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @ModelAttribute HealthRecord record, @RequestParam(required = false) Long userId) {
        record.setId(id);
        if (userId != null) {
            userRepository.findById(userId).ifPresent(record::setUser);
        }
        healthRecordRepository.save(record);
        return "redirect:/admin/health";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        healthRecordRepository.deleteById(id);
        return "redirect:/admin/health";
    }
} 