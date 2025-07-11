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
public class AddressController {

    @Autowired
    private CustomerRepository customerRepository;

    // 주소 수정 폼 보여주기
    @GetMapping("/address")
    public String showAddressForm(HttpSession session) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }
        return "member/address";  // templates/member/address.html
    }

    // 주소 수정 처리
    @PostMapping("/addressOk")
    public String updateAddress(
            @RequestParam String postcode,
            @RequestParam String address,
            @RequestParam String detailAddress,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        // 우편번호 + 주소 + 상세주소 합치기
        String fullAddr = postcode + " " + address + " " + detailAddress;
        loginUser.setCstmAddr(fullAddr);

        // DB 저장
        customerRepository.save(loginUser);

        redirectAttributes.addFlashAttribute("successMessage", "주소가 성공적으로 변경되었습니다.");
        return "redirect:/";
    }
}
