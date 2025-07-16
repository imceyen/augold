package com.global.augold.member.controller;

import com.global.augold.member.entity.CSInquiry;
import com.global.augold.member.entity.Customer;
import com.global.augold.member.repository.CSInquiryRepository;
import com.global.augold.member.repository.SequenceRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;  // 추가
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/support")
@RequiredArgsConstructor
public class SupportController {

    private final CSInquiryRepository inquiryRepository;
    private final SequenceRepository sequenceRepository;

    @GetMapping
    public String supportPage(Model model, HttpSession session) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");

        if (loginUser != null) {
            model.addAttribute("loginName", loginUser.getCstmName()); // Customer 엔티티의 이름 필드에 맞게 수정
        }

        return "member/support";
    }

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

        CSInquiry inquiry = new CSInquiry();  // SequenceRepository에서 직접 번호 받아서 설정
        String nextInqNumber = sequenceRepository.getNextSequence("CS_INQUIRY");
        inquiry.setInqNumber(nextInqNumber);
        inquiry.setCstmNumber(loginUser.getCstmNumber());
        inquiry.setInqCategory(category);
        inquiry.setInqTitle(title);
        inquiry.setInqContent(content);
        inquiry.setInqStatus("접수");

        inquiryRepository.save(inquiry);

        redirectAttributes.addFlashAttribute("successMessage", "문의가 접수되었습니다.");
        return "redirect:/support";
    }
}