package com.carecode.domain.admin.controller;

import com.carecode.domain.notification.entity.Notification;
import com.carecode.domain.notification.repository.NotificationRepository;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("notifications", notificationRepository.findAll());
        return "admin/notifications/list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("notification", new Notification());
        model.addAttribute("users", userRepository.findAll());
        return "admin/notifications/form";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute Notification notification, @RequestParam(required = false) Long userId) {
        if (userId != null) {
            userRepository.findById(userId).ifPresent(notification::setUser);
        }
        notificationRepository.save(notification);
        return "redirect:/admin/notifications";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("notification", notificationRepository.findById(id).orElse(null));
        return "admin/notifications/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("notification", notificationRepository.findById(id).orElse(null));
        model.addAttribute("users", userRepository.findAll());
        return "admin/notifications/form";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @ModelAttribute Notification notification, @RequestParam(required = false) Long userId) {
        notification.setId(id);
        if (userId != null) {
            userRepository.findById(userId).ifPresent(notification::setUser);
        }
        notificationRepository.save(notification);
        return "redirect:/admin/notifications";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        notificationRepository.deleteById(id);
        return "redirect:/admin/notifications";
    }
}
