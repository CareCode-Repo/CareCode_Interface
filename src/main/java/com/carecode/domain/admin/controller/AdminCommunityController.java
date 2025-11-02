package com.carecode.domain.admin.controller;

import com.carecode.domain.community.entity.Post;
import com.carecode.domain.community.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/community")
@RequiredArgsConstructor
public class AdminCommunityController {
    private final PostRepository postRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("posts", postRepository.findAll());
        return "admin/community/list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("post", new Post());
        return "admin/community/form";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute Post post) {
        postRepository.save(post);
        return "redirect:/admin/community";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("post", postRepository.findById(id).orElse(null));
        return "admin/community/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("post", postRepository.findById(id).orElse(null));
        return "admin/community/form";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @ModelAttribute Post post) {
        post.setId(id);
        postRepository.save(post);
        return "redirect:/admin/community";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        postRepository.deleteById(id);
        return "redirect:/admin/community";
    }
} 