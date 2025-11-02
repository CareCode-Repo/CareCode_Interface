package com.carecode.domain.admin.controller;

import com.carecode.domain.health.entity.Hospital;
import com.carecode.domain.health.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/hospitals")
@RequiredArgsConstructor
public class AdminHospitalController {
    private final HospitalRepository hospitalRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("hospitals", hospitalRepository.findAll());
        return "admin/hospitals/list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("hospital", new Hospital());
        return "admin/hospitals/form";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute Hospital hospital) {
        hospitalRepository.save(hospital);
        return "redirect:/admin/hospitals";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("hospital", hospitalRepository.findById(id).orElse(null));
        return "admin/hospitals/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("hospital", hospitalRepository.findById(id).orElse(null));
        return "admin/hospitals/form";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @ModelAttribute Hospital hospital) {
        hospital.setId(id);
        hospitalRepository.save(hospital);
        return "redirect:/admin/hospitals";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        hospitalRepository.deleteById(id);
        return "redirect:/admin/hospitals";
    }
} 