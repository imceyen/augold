package com.global.augold.member.controller;

import com.global.augold.member.entity.CSInquiry;
import com.global.augold.member.repository.CSInquiryRepository;
import com.global.augold.member.entity.Customer;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/support")
@RequiredArgsConstructor
public class SupportController {

    private final CSInquiryRepository inquiryRepository;

    /**
     * 고객센터 메인 페이지
     */
    @GetMapping
    public String supportPage() {
        // resources/templates/member/support.html
        return "member/support";
    }

    /**
     * 문의 접수 처리
     */
    @PostMapping("/submit")
    public String submitInquiry(@RequestParam String category,
                                @RequestParam String title,
                                @RequestParam String content,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {

        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        String inquiryNumber = generateInquiryNumber();

        CSInquiry inquiry = new CSInquiry();
        inquiry.setInqNumber(inquiryNumber);
        inquiry.setCstmNumber(loginUser.getCstmNumber());
        inquiry.setInqCategory(category);
        inquiry.setInqTitle(title);
        inquiry.setInqContent(content);
        inquiry.setInqStatus("접수");

        inquiryRepository.save(inquiry);

        redirectAttributes.addFlashAttribute("successMessage", "문의가 접수되었습니다.");
        return "redirect:/support";
    }

    /**
     * 문의번호 생성
     */
    private String generateInquiryNumber() {
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // YYYYMMDD
        String prefix = "INQ-" + today + "-";

        // 오늘 날짜의 가장 큰 번호를 조회
        String maxTodayInq = inquiryRepository.findMaxInquiryNumberForToday(prefix + "%");
        int nextNumber = 1;

        if (maxTodayInq != null) {
            // 예: INQ-20250714-00003 → 00003 → 3
            String lastSeq = maxTodayInq.substring(maxTodayInq.lastIndexOf("-") + 1);
            try {
                nextNumber = Integer.parseInt(lastSeq) + 1;
            } catch (NumberFormatException ignored) {}
        }

        return String.format("%s%05d", prefix, nextNumber);
    }
}
