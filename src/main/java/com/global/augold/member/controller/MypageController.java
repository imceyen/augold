package com.global.augold.member.controller;

import com.global.augold.member.entity.Customer;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller  // ⬅️ 이게 꼭 필요함!
public class MypageController {

    @GetMapping("/mypage")  // ⬅️ 슬래시(/)도 추가하는 게 안전
    public String mypage(HttpSession session, Model model) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");

        if (loginUser == null) {
            return "redirect:/login"; // 로그인 안 돼 있으면 로그인 페이지로
        }
        model.addAttribute("user", loginUser); // 템플릿으로 사용자 정보 전달
        return "member/mypage";  // templates/member/mypage.html
    }
}
