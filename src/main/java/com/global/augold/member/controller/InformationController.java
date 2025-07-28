package com.global.augold.member.controller;

import com.global.augold.member.entity.Customer;
// [삭제] 이 컨트롤러는 더 이상 CustomerService가 필요 없습니다.
// import com.global.augold.member.service.CustomerService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping; // [추가] RequestMapping import

@Controller
@RequiredArgsConstructor
@RequestMapping("/info") // [핵심] 이 컨트롤러의 모든 URL은 /info 로 시작합니다.
public class InformationController {

    // [삭제] 비밀번호 관련 로직은 모두 PasswordController로 이동했으므로 삭제합니다.

    // =================================================================
    // 주소 관리 관련
    // =================================================================

    /**
     * 주소 관리 페이지를 요청합니다. (최종 URL: GET /info/address)
     */
    @GetMapping("/address")
    public String showAddressPage(HttpSession session, Model model) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", loginUser);
        return "member/address"; // templates/member/address.html
    }


    // =================================================================
    // 연락처 수정 관련
    // =================================================================

    /**
     * 연락처 수정 페이지를 요청합니다. (최종 URL: GET /info/phone)
     */
    @GetMapping("/phone")
    public String showPhoneChangeForm(HttpSession session, Model model) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", loginUser);
        return "member/phone"; // templates/member/phone.html
    }
}