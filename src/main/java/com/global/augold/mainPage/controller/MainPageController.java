package com.global.augold.mainPage.controller;

import com.global.augold.goldPrice.dto.GoldPriceDTO;
import com.global.augold.goldPrice.Service.GoldPriceService;
import com.global.augold.mainPage.dto.MainPageInfoDTO;
import com.global.augold.mainPage.service.MainPageService;
import com.global.augold.member.entity.Customer;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class MainPageController {

    private final MainPageService mainPageService;
    private final GoldPriceService goldPriceService;

    @GetMapping("/")
    public String showMainPage(Model model, HttpSession session) {
        List<MainPageInfoDTO> products = mainPageService.getMainPageProducts();
        GoldPriceDTO todayPrice = goldPriceService.getTodayGoldPrice();

        GoldPriceDTO[] history = goldPriceService.getGoldPriceHistory();
        List<GoldPriceDTO> recent5 = Arrays.stream(history)
                .sorted((a, b) -> b.getEffectiveDate().compareTo(a.getEffectiveDate())) // 최신순
                .limit(5)
                .sorted(Comparator.comparing(GoldPriceDTO::getEffectiveDate)) // 오래된순
                .collect(Collectors.toList());

        List<String> chartLabels = recent5.stream()
                .map(p -> p.getEffectiveDate().substring(5))
                .collect(Collectors.toList());

        List<Double> chartData = recent5.stream()
                .map(p -> p.getPricePerGram() * 3.75 * 1.1)
                .collect(Collectors.toList());

        double basePrice = todayPrice.getPricePerGram() * 3.75 * 1.1;
        double price18k = basePrice * 0.75 * 0.83;
        double price14k = basePrice * 0.585 * 0.83;

        Customer loginCustomer = (Customer) session.getAttribute("loginUser");
        if (loginCustomer != null) {
            model.addAttribute("loginName", loginCustomer.getCstmName());
        }

        model.addAttribute("products", products);
        model.addAttribute("todayPrice", todayPrice);
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartData", chartData);
        model.addAttribute("price18k", price18k);
        model.addAttribute("price14k", price14k);

        return "main";
    }

}
