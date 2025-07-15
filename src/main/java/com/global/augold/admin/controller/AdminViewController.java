package com.global.augold.admin.controller;

import com.global.augold.admin.service.AdminService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminViewController {

    private final AdminService adminService;

    public AdminViewController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/inquiry/{inqNumber}")
    public String inquiryDetail(@PathVariable String inqNumber, Model model) {
        var inquiry = adminService.getInquiry(inqNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문의입니다."));
        model.addAttribute("inquiry", inquiry);
        return "admin/inquiry-detail"; // templates/admin/inquiry-detail.html 위치
    }
}
