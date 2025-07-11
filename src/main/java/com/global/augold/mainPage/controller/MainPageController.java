package com.global.augold.mainPage.controller;

import com.global.augold.goldPrice.dto.GoldPriceDTO;
// import com.global.augold.goldPrice.Service.GoldPriceService;
import com.global.augold.mainPage.dto.MainPageInfoDTO;
import com.global.augold.mainPage.service.MainPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Arrays;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainPageController {

    private final MainPageService mainPageService;
    //private final GoldPriceService goldPriceService;

    @GetMapping("/")
    public String showMainPage(Model model) {
        List<MainPageInfoDTO> products = mainPageService.getMainPageProducts();
        //GoldPriceDTO todayPrice = goldPriceService.getTodayGoldPrice();
        //GoldPriceDTO[] historyPrices = goldPriceService.getGoldPriceHistory();

        model.addAttribute("products", products);
        //model.addAttribute("todayPrice", todayPrice);
        //model.addAttribute("historyPrices", Arrays.asList(historyPrices));

        return "main"; // templates/main.html
    }
}
