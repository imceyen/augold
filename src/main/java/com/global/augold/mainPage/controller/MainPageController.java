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
        // 1. 상품 목록 가져오기 (스케줄러가 이미 골드바 가격 업데이트 완료)
        List<MainPageInfoDTO> products = mainPageService.getMainPageProducts();

        // 2. 금 시세 가져오기 (엑셀에서)
        GoldPriceDTO todayPrice = goldPriceService.getTodayGoldPrice();

        // 🔥 3. 골드바 중복 계산 부분 삭제 (스케줄러가 이미 처리)

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

        // 5. 살때/팔때 가격 계산
        double basePrice = todayPrice.getPricePerGram() * 3.75 * 1.1; // 순금 살때 가격

        // 🔥 새로 추가: 팔때 가격들
        double sellPrice24k = basePrice * 0.85;  // 순금 팔때 (15% 할인)

        // 18K, 14K 살때/팔때 가격
        double buyPrice18k = basePrice * 0.75;  // 18K 살때
        double sellPrice18k = buyPrice18k * 0.83; // 18K 팔때

        double buyPrice14k = basePrice * 0.585; // 14K 살때
        double sellPrice14k = buyPrice14k * 0.83; // 14K 팔때

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

        // 🔥 살때/팔때 가격들
        model.addAttribute("buyPrice24k", basePrice);
        model.addAttribute("sellPrice24k", sellPrice24k);
        model.addAttribute("buyPrice18k", buyPrice18k);
        model.addAttribute("sellPrice18k", sellPrice18k);
        model.addAttribute("buyPrice14k", buyPrice14k);
        model.addAttribute("sellPrice14k", sellPrice14k);

        return "main";
    }
}