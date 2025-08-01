package com.global.augold.member.controller;

import com.global.augold.member.entity.Customer;
import com.global.augold.member.service.CustomerService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/password") // 이 컨트롤러의 모든 경로는 /password로 시작합니다.
public class PasswordController {

    private final CustomerService customerService;

    // 최종 URL: GET /password
    @GetMapping
    public String showPasswordForm(HttpSession session, Model model) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        // 로그인 이름을 모델에 추가 → 헤더에서 사용
        model.addAttribute("loginName", loginUser.getCstmName());

        // 리다이렉트된 메시지가 있다면 뷰로 전달
        if (model.containsAttribute("errorMessage")) {
            model.addAttribute("errorMessage", model.getAttribute("errorMessage"));
        }
        if (model.containsAttribute("successMessage")) {
            model.addAttribute("successMessage", model.getAttribute("successMessage"));
        }

        return "member/password";
    }

    // 최종 URL: POST /password
    @PostMapping
    public String updatePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {

        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        model.addAttribute("loginName", loginUser.getCstmName());

        // [신규] 새 비밀번호가 현재 비밀번호와 동일한지 확인하는 로직 추가
        if (currentPassword.equals(newPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "새 비밀번호는 현재 비밀번호와 다르게 설정해야 합니다.");
            return "redirect:/password";
        }

        // 1. 현재 비밀번호가 맞는지 서비스 계층을 통해 확인
        if (!customerService.checkPassword(loginUser.getCstmNumber(), currentPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "현재 비밀번호가 일치하지 않습니다.");
            return "redirect:/password";
        }

        // 2. 새 비밀번호와 비밀번호 확인이 일치하는지 확인
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "새 비밀번호가 일치하지 않습니다.");
            return "redirect:/password";
        }

        // 3. 모든 검증 통과 시, 서비스 계층에 비밀번호 업데이트 요청
        customerService.updatePassword(loginUser.getCstmNumber(), newPassword);

        redirectAttributes.addFlashAttribute("successMessage", "비밀번호가 성공적으로 수정되었습니다.");
        return "redirect:/info/member/information"; // 성공 시 마이페이지로 이동
    }
}