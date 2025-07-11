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
public class MemberDeleteController {

    @Autowired
    private CustomerRepository customerRepository;

    // 회원탈퇴 폼 보여주기
    @GetMapping("/member/delete")
    public String showDeleteForm(HttpSession session) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }
        return "member/delete"; // templates/member/delete.html
    }

    // 회원탈퇴 처리
    @PostMapping("/member/delete")
    public String deleteMember(@RequestParam String password,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        // 비밀번호 확인
        if (!loginUser.getCstmPwd().equals(password)) {
            redirectAttributes.addFlashAttribute("errorMessage", "비밀번호가 일치하지 않습니다.");
            return "redirect:/member/delete";
        }

        // 회원 삭제
        customerRepository.delete(loginUser);

        // 세션 무효화 (로그아웃)
        session.invalidate();

        redirectAttributes.addFlashAttribute("successMessage", "회원 탈퇴가 정상 처리되었습니다.");
        return "redirect:/main";
    }
}
