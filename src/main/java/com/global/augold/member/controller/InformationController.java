package com.global.augold.member.controller;

import com.global.augold.member.entity.Customer;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/info")
public class InformationController {

    @GetMapping("/member/information")
    public String showInformationPage(HttpSession session, Model model) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        model.addAttribute("loginName", loginUser.getCstmName());
        return "member/information";
    }

    @GetMapping("/address")
    public String showAddressPage(HttpSession session, Model model) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        model.addAttribute("loginName", loginUser.getCstmName());
        return "member/address"; // templates/member/address.html
    }


    @GetMapping("/phone")
    public String showPhoneChangeForm(HttpSession session, Model model) {
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }
        model.addAttribute("loginName", loginUser.getCstmName());
        return "member/phone"; // templates/member/phone.html
    }
}