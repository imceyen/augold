package com.global.augold.member.controller;

import com.global.augold.member.entity.Customer;
import com.global.augold.member.repository.CustomerRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class LoginController {

    @Autowired
    private CustomerRepository customerRepository;

    @PostMapping("/loginOk")
    public String login(
            @RequestParam String cstmId,
            @RequestParam String cstmPwd,
            @RequestParam(required = false) String returnUrl,
            HttpSession session
    ) {
        // 관리자 로그인 체크
        if ("admin".equals(cstmId) && "1234".equals(cstmPwd)) {
            session.setAttribute("loginUser", "admin"); // 문자열로 저장
            return "redirect:/admin";  // 관리자 페이지 URL
        }

        // 일반 사용자 로그인 처리
        Optional<Customer> optCustomer = customerRepository.findByCstmIdAndCstmPwd(cstmId, cstmPwd);

        if (optCustomer.isPresent()) {
            // 로그인 성공 - 세션에 회원 정보 저장
            session.setAttribute("loginUser", optCustomer.get());

            //returnUrl 이 있으면 해당 페이지로 이동하게 추가, 없으면 메인으로
            if (returnUrl != null && !returnUrl.isEmpty()){
                return "redirect:" + returnUrl;
            }else {
                return "redirect:/";
            }
        } else {
            // 로그인 실패시에도 returnurl 유지
            String redirectUrl = "redirect:/login?error=true";
            if (returnUrl != null && !returnUrl.isEmpty()){
                redirectUrl += "&returnUrl=" + returnUrl;
            }
            // 로그인 실패 - 쿼리 파라미터로 error=true 전달
            return redirectUrl;
        }
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "member/login";  // 로그인 폼 뷰 이름
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
    @GetMapping("/admin")
    public String adminPage(HttpSession session) {
        Object user = session.getAttribute("loginUser");
        if (user == null || !"admin".equals(user)) {
            return "redirect:/login"; // 비로그인 접근 방지
        }
        return "admin/admin"; // Thymeleaf 기준. 또는 static/admin.html 이면 "redirect:/admin.html"
    }
}
