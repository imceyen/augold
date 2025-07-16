package com.global.augold.goldPrice.Controller;

import com.global.augold.goldPrice.dto.GoldPriceDTO;
import com.global.augold.goldPrice.Service.GoldPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class GoldPriceController {

    private final GoldPriceService goldPriceService;

    @GetMapping("/gold-price")
    public String getGoldPricePage(Model model) {
        GoldPriceDTO today = goldPriceService.getTodayGoldPrice();
        GoldPriceDTO[] history = goldPriceService.getGoldPriceHistory();

        // ✅ 모델에 오늘 시세 전달
        model.addAttribute("todayPrice", today);

        // ✅ 18K / 14K 매입가 계산
        double pricePerGram = today.getPricePerGram();
        model.addAttribute("price18k", (int) Math.round(pricePerGram * 0.75 * 3.75));
        model.addAttribute("price14k", (int) Math.round(pricePerGram * 0.585 * 3.75));

        // ✅ 차트용 라벨 & 데이터
        List<String> chartLabels = new ArrayList<>();
        List<Integer> chartData = new ArrayList<>();

        for (int i = history.length - 1; i >= 0; i--) {
            chartLabels.add("\"" + history[i].getEffectiveDate() + "\"");  // 날짜 문자열로 감싸기
            chartData.add((int) Math.round(history[i].getPricePerGram() * 3.75));
        }

        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartData", chartData);

        return "gold_price";  // gold_price.html 템플릿 렌더링
    }
}
