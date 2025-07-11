package com.global.augold.member.controller;

import com.global.augold.member.entity.Customer;
import com.global.augold.member.repository.CustomerRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class LoginController {

    @Autowired
    private CustomerRepository customerRepository;

    @PostMapping("/loginOk")
    public String login(
            @RequestParam String cstmId,
            @RequestParam String cstmPwd,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Optional<Customer> optCustomer = customerRepository.findByCstmIdAndCstmPwd(cstmId, cstmPwd);

        if (optCustomer.isPresent()) {
            // 로그인 성공 - 세션에 회원 정보 저장
            session.setAttribute("loginUser", optCustomer.get());
            return "redirect:/";
        } else {
            // 로그인 실패 - 에러 메시지 전달 후 로그인 페이지로
            redirectAttributes.addFlashAttribute("error", "아이디 또는 비밀번호가 일치하지 않습니다.");
            return "redirect:/login";
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
}
