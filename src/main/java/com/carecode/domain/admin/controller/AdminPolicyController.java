package com.carecode.domain.admin.controller;

import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/policies")
@RequiredArgsConstructor
public class AdminPolicyController {
    private final PolicyRepository policyRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("policies", policyRepository.findAll());
        return "admin/policies/list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("policy", new Policy());
        return "admin/policies/form";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute Policy policy) {
        policyRepository.save(policy);
        return "redirect:/admin/policies";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("policy", policyRepository.findById(id).orElse(null));
        return "admin/policies/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("policy", policyRepository.findById(id).orElse(null));
        return "admin/policies/form";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @ModelAttribute Policy policy) {
        policy.setId(id);
        policyRepository.save(policy);
        return "redirect:/admin/policies";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        policyRepository.deleteById(id);
        return "redirect:/admin/policies";
    }
} 