package com.global.augold.member.controller;

import com.global.augold.member.entity.Customer;
import com.global.augold.member.service.CustomerService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class PhoneController {

    private final CustomerService customerService;

    /**
     * 연락처 변경 폼 페이지를 보여주는 메서드 (GET /phone)
     * 이 메서드가 HTML 파일에 loginUser 정보를 전달하는 역할을 합니다.
     */
    @GetMapping("/phone")
    public String showPhoneForm(HttpSession session, Model model) {
        // 1. 세션에서 현재 로그인한 사용자 정보를 가져옵니다.
        Customer loginUser = (Customer) session.getAttribute("loginUser");

        // 2. 만약 로그인 정보가 없으면, 로그인 페이지로 보냅니다. (오류 방지)
        if (loginUser == null) {
            return "redirect:/login";
        }

        // 3. [가장 중요한 부분] HTML 파일에서 사용할 수 있도록 loginUser 정보를 "loginUser"라는 이름으로 모델에 담아줍니다.
        model.addAttribute("loginUser", loginUser);

        // 4. member 폴더 밑의 phone.html 파일을 화면에 보여줍니다.
        return "member/phone";
    }

    /**
     * 연락처 변경 요청을 처리하는 메서드 (POST /phoneOk)
     */
    @PostMapping("/phoneOk")
    public String updatePhone(@RequestParam String newPhone,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {

        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        // 새 연락처가 현재 연락처와 동일한지 확인
        if (loginUser.getCstmPhone() != null && loginUser.getCstmPhone().equals(newPhone)) {
            redirectAttributes.addFlashAttribute("errorMessage", "새 연락처는 현재 연락처와 다르게 설정해야 합니다.");
            return "redirect:/phone";
        }

        // 연락처 유효성 검사 (010-XXXX-XXXX 형식)
        if (!newPhone.matches("^010-\\d{4}-\\d{4}$")) {
            redirectAttributes.addFlashAttribute("errorMessage", "올바른 휴대폰 번호 형식이 아닙니다. (예: 010-1234-5678)");
            return "redirect:/phone";
        }

        // 서비스에 연락처 업데이트 요청
        customerService.updatePhone(loginUser.getCstmNumber(), newPhone);

        // 성공 메시지와 함께 메인 페이지로 이동
        redirectAttributes.addFlashAttribute("successMessage", "연락처가 성공적으로 수정되었습니다.");
        return "redirect:/";
    }
}