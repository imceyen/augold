package com.global.augold.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 관리자 페이지 접속용 뷰 컨트롤러
 */
@Controller
public class AdminViewController {

    @GetMapping("/admin")
    public String showAdminPage() {
        return "admin/admin";  // templates/admin.html 반환
    }
}
