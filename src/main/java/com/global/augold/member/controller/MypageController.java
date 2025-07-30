package com.global.augold.member.controller;

import com.global.augold.member.entity.CSInquiry;
import com.global.augold.member.entity.Customer;
import com.global.augold.member.repository.CSInquiryRepository;
import com.global.augold.member.service.CustomerService; // [수정] CustomerService를 import 합니다.
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MypageController {

    // 의존성 주입
    private final CSInquiryRepository inquiryRepository;
    private final CustomerService customerService; // [수정] CustomerService를 주입받습니다.

    // 마이페이지 기본 화면 (변경 없음)
    @GetMapping("/mypage")
    public String mypage(HttpSession session, Model model) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", loginUser);
        model.addAttribute("loginName", loginUser.getCstmName());
        return "member/mypage";
    }


    // =================================================================
    // [신규 추가] 회원 정보 수정을 위한 컨트롤러 메소드
    // =================================================================

    /**
     * 회원 정보 수정을 위해 비밀번호 확인 페이지를 요청합니다. (GET /member/confirm)
     */
    @GetMapping("/member/confirm")
    public String showConfirmPasswordForm(HttpSession session) {
        if (session.getAttribute("loginUser") == null) {
            return "redirect:/login";
        }
        // templates/member/confirm.html 뷰를 반환합니다.
        return "member/confirm";
    }

    /**
     * 사용자가 입력한 비밀번호를 검증합니다. (POST /member/confirm)
     */
    @PostMapping("/member/confirm")
    public String processConfirmPassword(
            @RequestParam("password") String password, // 폼에서 전송된 비밀번호
            HttpSession session,
            Model model) { // 실패 시 오류 메시지를 전달하기 위해 Model 사용

        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        // CustomerService의 checkPassword 메소드를 호출하여 비밀번호 일치 여부 확인
        boolean isPasswordCorrect = customerService.checkPassword(loginUser.getCstmNumber(), password);

        if (isPasswordCorrect) {
            // 비밀번호가 맞으면, 회원 정보 수정 페이지로 리다이렉트합니다.
            return "redirect:/member/information";
        } else {
            // 비밀번호가 틀리면, 에러 메시지와 함께 다시 비밀번호 확인 페이지를 보여줍니다.
            model.addAttribute("errorMessage", "비밀번호가 일치하지 않습니다.");
            return "member/confirm";
        }
    }

    /**
     * 비밀번호 확인 성공 후, 실제 회원 정보 수정 페이지를 보여줍니다. (GET /member/information)
     */
    @GetMapping("/member/information")
    public String showInformationPage(HttpSession session) {
        if (session.getAttribute("loginUser") == null) {
            // 중간에 세션이 만료되었을 경우를 대비
            return "redirect:/login";
        }
        // templates/member/information.html 뷰를 반환합니다.
        return "member/information";
    }


    // =================================================================
    // 기존 문의(Inquiry) 관련 기능 (변경 없음)
    // =================================================================

    @GetMapping("/mypage/inquiries")
    public String myInquiries(HttpSession session, Model model,
                              @RequestParam(required = false) String keyword) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        List<CSInquiry> inquiries;

        if (keyword != null && !keyword.trim().isEmpty()) {
            inquiries = inquiryRepository.findByCstmNumberAndInqTitleContainingIgnoreCaseOrderByInqDateDesc(
                    loginUser.getCstmNumber(), keyword);
        } else {
            inquiries = inquiryRepository.findByCstmNumberOrderByInqDateDesc(loginUser.getCstmNumber());
        }

        model.addAttribute("inquiries", inquiries);
        model.addAttribute("keyword", keyword);

        return "member/mypage_inquiries";
    }

    @GetMapping("/mypage/inquiries/{inqNumber}")
    public String inquiryDetail(@PathVariable String inqNumber, HttpSession session, Model model) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        CSInquiry inquiry = inquiryRepository.findById(inqNumber).orElse(null);
        if (inquiry == null || !inquiry.getCstmNumber().equals(loginUser.getCstmNumber())) {
            return "redirect:/mypage/inquiries";
        }

        model.addAttribute("inquiry", inquiry);
        return "member/mypage_inquiry_detail";
    }

    @GetMapping("/mypage/inquiries/{inqNumber}/edit")
    public String editInquiryForm(@PathVariable String inqNumber, HttpSession session, Model model) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) return "redirect:/login";

        CSInquiry inquiry = inquiryRepository.findById(inqNumber).orElse(null);
        if (inquiry == null || !inquiry.getCstmNumber().equals(loginUser.getCstmNumber())) {
            return "redirect:/mypage/inquiries";
        }

        model.addAttribute("inquiry", inquiry);
        return "member/mypage_inquiry_edit";
    }

    @PostMapping("/mypage/inquiries/{inqNumber}/edit")
    public String editInquirySubmit(@PathVariable String inqNumber, @RequestParam String inqTitle,
                                    @RequestParam String inqContent, HttpSession session) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) return "redirect:/login";

        CSInquiry inquiry = inquiryRepository.findById(inqNumber).orElse(null);
        if (inquiry == null || !inquiry.getCstmNumber().equals(loginUser.getCstmNumber())) {
            return "redirect:/mypage/inquiries";
        }

        inquiry.setInqTitle(inqTitle);
        inquiry.setInqContent(inqContent);
        inquiryRepository.save(inquiry);

        return "redirect:/mypage/inquiries/" + inqNumber;
    }

    @PostMapping("/mypage/inquiries/{inqNumber}/delete")
    public String deleteInquiry(@PathVariable String inqNumber, HttpSession session) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) return "redirect:/login";

        CSInquiry inquiry = inquiryRepository.findById(inqNumber).orElse(null);
        if (inquiry == null || !inquiry.getCstmNumber().equals(loginUser.getCstmNumber())) {
            return "redirect:/mypage/inquiries";
        }

        inquiryRepository.delete(inquiry);
        return "redirect:/mypage/inquiries";
    }

    @GetMapping("/mypage/inquiries/{inqNumber}/confirm")
    public String confirmPasswordForm(@PathVariable String inqNumber, Model model) {
        model.addAttribute("inqNumber", inqNumber);
        return "member/mypage_inquiry_confirm";
    }

    @PostMapping("/mypage/inquiries/{inqNumber}/confirm")
    public String confirmPassword(@PathVariable String inqNumber,
                                  @RequestParam String password,
                                  @RequestParam String action,
                                  HttpSession session,
                                  Model model) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) return "redirect:/login";

        if (!loginUser.getCstmPwd().equals(password)) {
            model.addAttribute("inqNumber", inqNumber);
            model.addAttribute("error", "비밀번호가 일치하지 않습니다.");
            return "member/mypage_inquiry_confirm";
        }

        if ("edit".equals(action)) {
            return "redirect:/mypage/inquiries/" + inqNumber + "/edit";
        } else if ("delete".equals(action)) {
            CSInquiry inquiry = inquiryRepository.findById(inqNumber).orElse(null);
            if (inquiry != null && inquiry.getCstmNumber().equals(loginUser.getCstmNumber())) {
                inquiryRepository.delete(inquiry);
            }
            return "redirect:/mypage/inquiries";
        }

        return "redirect:/mypage/inquiries";
    }
}