package com.global.augold.mainPage.controller;

import com.global.augold.mainPage.dto.MainPageInfoDTO;
import com.global.augold.mainPage.service.MainPageService;
import com.global.augold.member.entity.Customer;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainPageController {

    private final MainPageService mainPageService;

    @GetMapping("/")
    public String showMainPage(Model model, HttpSession session) {
        List<MainPageInfoDTO> products = mainPageService.getMainPageProducts();
        model.addAttribute("products", products);

        Object loginUserObj = session.getAttribute("loginUser");
        if (loginUserObj instanceof Customer) {
            Customer loginCustomer = (Customer) loginUserObj;
            model.addAttribute("loginName", loginCustomer.getCstmName());
        }
        // 관리자 로그인 시 loginUser가 String "admin"이거나 다른 타입일 경우 무시하고 이름 출력 안함

        return "main";
    }
}
