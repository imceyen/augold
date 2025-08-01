package com.global.augold.member.controller;

import com.global.augold.member.entity.Customer;
import com.global.augold.member.repository.CSInquiryRepository;
import com.global.augold.member.repository.CustomerRepository;
import com.global.augold.member.service.CustomerService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

@Controller
public class MemberDeleteController {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CSInquiryRepository inquiryRepository;

    @Autowired
    private CustomerService customerService;


    // 회원탈퇴 폼 보여주기
    @GetMapping("/member/delete")
    public String showDeleteForm(HttpSession session, Model model) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        model.addAttribute("loginName", loginUser.getCstmName());
        return "member/delete"; // templates/member/delete.html
    }

    // 회원탈퇴 처리
    @PostMapping("/member/delete")
    @Transactional
    public String deleteMember(@RequestParam String password,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        // 비밀번호 확인
        if (!customerService.checkPassword(loginUser.getCstmNumber(), password)) {
            redirectAttributes.addFlashAttribute("errorMessage", "비밀번호가 일치하지 않습니다.");
            return "redirect:/member/delete";
        }

        try {
            // 👇 1. 먼저 관련된 문의사항들을 삭제
            inquiryRepository.deleteByCstmNumber(loginUser.getCstmNumber());

            // 👇 2. 그 다음 고객 정보 삭제
            customerRepository.delete(loginUser);

            // 3. 세션 무효화
            session.invalidate();

            redirectAttributes.addFlashAttribute("successMessage", "회원 탈퇴가 완료되었습니다.");
            return "redirect:/";

        } catch (Exception e) {
            System.out.println("회원 탈퇴 실패: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "탈퇴 처리 중 오류가 발생했습니다.");
            return "redirect:/member/delete";
        }
    }
}
