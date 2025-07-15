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
        // 1. 상품 목록 가져오기
        List<MainPageInfoDTO> products = mainPageService.getMainPageProducts();

        // 2. 금 시세 가져오기
        GoldPriceDTO todayPrice = goldPriceService.getTodayGoldPrice();
        double goldPricePerGram = todayPrice.getPricePerGram() * 1.1;

        // ✅ 3. 골드바 상품에 한해 가격 갱신
        products = products.stream()
                .map(p -> {
                    if ("0002".equals(p.getCtgrId()) && p.getGoldWeight() != null) {
                        double newPrice = p.getGoldWeight() * goldPricePerGram;
                        p.setFinalPrice(newPrice);
                    }
                    return p;
                })
                .collect(Collectors.toList());

        // 4. 금 시세 히스토리 (최근 5일 → 오래된순)
        GoldPriceDTO[] history = goldPriceService.getGoldPriceHistory();
        List<GoldPriceDTO> recent5 = Arrays.stream(history)
                .sorted((a, b) -> b.getEffectiveDate().compareTo(a.getEffectiveDate())) // 최신순
                .limit(5)
                .sorted(Comparator.comparing(GoldPriceDTO::getEffectiveDate)) // 오래된순으로 다시 정렬
                .collect(Collectors.toList());

        List<String> chartLabels = recent5.stream()
                .map(p -> p.getEffectiveDate().substring(5)) // MM-DD 형식
                .collect(Collectors.toList());

        List<Double> chartData = recent5.stream()
                .map(p -> p.getPricePerGram() * 3.75 * 1.1) // 기준 중량 3.75g
                .collect(Collectors.toList());

        // 5. 14K, 18K 기준 가격 계산
        double basePrice = todayPrice.getPricePerGram() * 3.75 * 1.1;
        double price18k = basePrice * 0.75 * 0.83;
        double price14k = basePrice * 0.585 * 0.83;

        // 6. 로그인 사용자 정보 전달 (고객만)
        Object loginUserObj = session.getAttribute("loginUser");
        if (loginUserObj instanceof Customer customer) {
            model.addAttribute("loginName", customer.getCstmName());
        }

        // 7. 모델 데이터 전달
        model.addAttribute("products", products);
        model.addAttribute("todayPrice", todayPrice);
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartData", chartData);
        model.addAttribute("price18k", price18k);
        model.addAttribute("price14k", price14k);

        return "main";
    }
}
