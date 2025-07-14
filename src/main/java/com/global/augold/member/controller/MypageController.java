package com.global.augold.member.controller;

import com.global.augold.member.entity.CSInquiry;
import com.global.augold.member.entity.Customer;
import com.global.augold.member.repository.CSInquiryRepository;
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

    // 문의 저장소 주입 (JPA Repository)
    private final CSInquiryRepository inquiryRepository;

    // 마이페이지 기본 화면 - 로그인 확인 후 사용자 정보 모델에 담아 뷰로 전달
    @GetMapping("/mypage")
    public String mypage(HttpSession session, Model model) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) {  // 로그인 안 되어 있으면 로그인 페이지로 리다이렉트
            return "redirect:/login";
        }

        model.addAttribute("user", loginUser); // 사용자 정보 전달
        return "member/mypage";  // 마이페이지 뷰 이름
    }

    // 로그인한 사용자의 문의 목록 조회 및 키워드 검색 기능 포함
    @GetMapping("/mypage/inquiries")
    public String myInquiries(HttpSession session, Model model,
                              @RequestParam(required = false) String keyword) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        List<CSInquiry> inquiries;

        if (keyword != null && !keyword.trim().isEmpty()) {
            // 키워드가 있으면 제목에 해당 키워드 포함된 문의 검색
            inquiries = inquiryRepository.findByCstmNumberAndInqTitleContainingIgnoreCaseOrderByInqDateDesc(
                    loginUser.getCstmNumber(), keyword);
        } else {
            // 키워드 없으면 사용자의 전체 문의 목록을 최신순으로 조회
            inquiries = inquiryRepository.findByCstmNumberOrderByInqDateDesc(loginUser.getCstmNumber());
        }

        model.addAttribute("inquiries", inquiries);
        model.addAttribute("keyword", keyword);

        return "member/mypage_inquiries"; // 문의 목록 뷰 이름
    }

    // 문의 상세 조회 - 로그인 확인 + 해당 문의가 사용자 소유인지 검증
    @GetMapping("/mypage/inquiries/{inqNumber}")
    public String inquiryDetail(@PathVariable String inqNumber, HttpSession session, Model model) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        // 문의 번호로 데이터 조회
        CSInquiry inquiry = inquiryRepository.findById(inqNumber).orElse(null);
        // 문의가 없거나 본인 소유가 아니면 목록 페이지로 리다이렉트
        if (inquiry == null || !inquiry.getCstmNumber().equals(loginUser.getCstmNumber())) {
            return "redirect:/mypage/inquiries";
        }

        model.addAttribute("inquiry", inquiry);
        return "member/mypage_inquiry_detail"; // 상세 보기 뷰 이름
    }

    // 문의 수정 폼 요청 - 로그인 및 소유권 체크 후 수정 폼으로 이동
    @GetMapping("/mypage/inquiries/{inqNumber}/edit")
    public String editInquiryForm(@PathVariable String inqNumber, HttpSession session, Model model) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) return "redirect:/login";

        CSInquiry inquiry = inquiryRepository.findById(inqNumber).orElse(null);
        if (inquiry == null || !inquiry.getCstmNumber().equals(loginUser.getCstmNumber())) {
            return "redirect:/mypage/inquiries";
        }

        model.addAttribute("inquiry", inquiry);
        return "member/mypage_inquiry_edit";  // 수정 폼 뷰 이름
    }

    // 수정 폼 제출 처리 - 수정 내용 저장 후 상세 페이지로 리다이렉트
    @PostMapping("/mypage/inquiries/{inqNumber}/edit")
    public String editInquirySubmit(@PathVariable String inqNumber, @RequestParam String inqTitle,
                                    @RequestParam String inqContent, HttpSession session) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) return "redirect:/login";

        CSInquiry inquiry = inquiryRepository.findById(inqNumber).orElse(null);
        if (inquiry == null || !inquiry.getCstmNumber().equals(loginUser.getCstmNumber())) {
            return "redirect:/mypage/inquiries";
        }

        // 제목과 내용 업데이트 후 저장
        inquiry.setInqTitle(inqTitle);
        inquiry.setInqContent(inqContent);
        inquiryRepository.save(inquiry);

        return "redirect:/mypage/inquiries/" + inqNumber;
    }

    // 문의 삭제 요청 처리 - 로그인, 소유권 검증 후 삭제
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

    // 비밀번호 확인 폼 요청 (수정/삭제 전에 비밀번호 확인용)
    @GetMapping("/mypage/inquiries/{inqNumber}/confirm")
    public String confirmPasswordForm(@PathVariable String inqNumber, Model model) {
        model.addAttribute("inqNumber", inqNumber);
        return "member/mypage_inquiry_confirm"; // 비밀번호 확인 뷰 이름
    }

    // 비밀번호 확인 처리 - 일치하면 수정 또는 삭제 페이지 혹은 삭제 처리로 이동
    @PostMapping("/mypage/inquiries/{inqNumber}/confirm")
    public String confirmPassword(@PathVariable String inqNumber,
                                  @RequestParam String password,
                                  @RequestParam String action,
                                  HttpSession session,
                                  Model model) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) return "redirect:/login";

        // 로그인된 사용자의 비밀번호와 입력받은 비밀번호 일치 여부 체크
        if (!loginUser.getCstmPwd().equals(password)) {
            // 비밀번호 불일치시 오류 메시지 출력 후 비밀번호 확인 폼 재표시
            model.addAttribute("inqNumber", inqNumber);
            model.addAttribute("error", "비밀번호가 일치하지 않습니다.");
            return "member/mypage_inquiry_confirm";
        }

        // 비밀번호 확인 성공 시 액션 분기
        if ("edit".equals(action)) {
            // 수정 폼 페이지로 리다이렉트
            return "redirect:/mypage/inquiries/" + inqNumber + "/edit";
        } else if ("delete".equals(action)) {
            // 삭제 처리 후 목록 페이지로 리다이렉트
            CSInquiry inquiry = inquiryRepository.findById(inqNumber).orElse(null);
            if (inquiry != null && inquiry.getCstmNumber().equals(loginUser.getCstmNumber())) {
                inquiryRepository.delete(inquiry);
            }
            return "redirect:/mypage/inquiries";
        }

        // 액션 값이 없거나 다르면 목록 페이지로
        return "redirect:/mypage/inquiries";
    }
}