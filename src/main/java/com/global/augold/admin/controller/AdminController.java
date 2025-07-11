package com.global.augold.admin.controller;

import com.global.augold.admin.dto.AdminLoginRequestDTO;
import com.global.augold.admin.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 전용 컨트롤러
 * - 로그인, 회원 관리, 상품 관리, 통계 등
 */

@RestController
@RequestMapping("/admin")

public class AdminController {

    private final AdminService adminService;

    // 생성자 주입 방식 (권장)
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * 관리자 로그인 API
     * POST /admin/login
     */
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody AdminLoginRequestDTO requestDto) {
        // 서비스 계층에 로그인 요청 위임
        boolean success = adminService.login(requestDto.getUsername(), requestDto.getPassword());

        if (success) {
            return ResponseEntity.ok("로그인 성공!");
        } else {
            return ResponseEntity.status(401).body("로그인 실패");
        }
    }


}

