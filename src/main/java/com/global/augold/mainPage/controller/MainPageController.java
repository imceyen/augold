package com.global.augold.mainPage.controller;

import com.global.augold.mainPage.dto.MainPageInfoDTO;
import com.global.augold.mainPage.service.MainPageService;
import com.global.augold.member.entity.Customer;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainPageController {

    private final MainPageService mainPageService;

    @GetMapping("/")
    public String showMainPage(Model model, HttpSession session) {
        List<MainPageInfoDTO> products = mainPageService.getMainPageProducts();
        model.addAttribute("products", products);

        Customer loginCustomer = (Customer) session.getAttribute("loginUser");
        if (loginCustomer != null) {
            model.addAttribute("loginName", loginCustomer.getCstmName());
        }

        return "main";
    }

}
