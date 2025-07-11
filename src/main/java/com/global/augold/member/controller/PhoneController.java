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

@Controller
public class PhoneController {

    @Autowired
    private CustomerRepository customerRepository;

    // 전화번호 수정 폼
    @GetMapping("/phone")
    public String showPhoneForm(HttpSession session) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }
        return "member/phone"; // templates/member/phone.html
    }

    // 전화번호 수정 처리
    @PostMapping("/phoneOk")
    public String PhoneOk(@RequestParam String newPhone,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        // 전화번호 유효성 검사는 필요에 따라 추가

        loginUser.setCstmPhone(newPhone);
        customerRepository.save(loginUser); // DB 반영

        redirectAttributes.addFlashAttribute("successMessage", "전화번호가 성공적으로 변경되었습니다.");
        return "redirect:/";
    }
}
