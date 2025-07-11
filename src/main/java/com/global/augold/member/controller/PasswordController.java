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
public class PasswordController {

    @Autowired
    private CustomerRepository customerRepository;

    // 비밀번호 변경 폼
    @GetMapping("/password")
    public String showPasswordForm(HttpSession session) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }
        return "member/password"; // templates/member/password.html
    }

    // 비밀번호 변경 처리
    @PostMapping("/passwordinsert")
    public String updatePassword(@RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");

        if (loginUser == null) {
            return "redirect:/login";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "비밀번호가 일치하지 않습니다.");
            return "redirect:/password";
        }

        loginUser.setCstmPwd(newPassword);
        customerRepository.save(loginUser); // DB 반영

        redirectAttributes.addFlashAttribute("successMessage", "비밀번호가 수정되었습니다.");
        return "redirect:/main";
    }
}
