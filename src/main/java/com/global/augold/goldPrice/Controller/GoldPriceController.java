package com.global.augold.goldPrice.Controller;

import com.global.augold.goldPrice.dto.GoldPriceDTO;
import com.global.augold.goldPrice.Service.GoldPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;
import java.util.ArrayList;


@Controller
@RequiredArgsConstructor
public class GoldPriceController {

    private final GoldPriceService goldPriceService;


    @GetMapping("/gold-price")
    public String getGoldPricePage(Model model) {
        GoldPriceDTO today = goldPriceService.getTodayGoldPrice();
        GoldPriceDTO[] history = goldPriceService.getGoldPriceHistory();

        model.addAttribute("todayPrice", today);

        double pricePerGram = today.getPricePerGram(); // ✅ 수정: 타입 일치
        model.addAttribute("price18k", (int) Math.round(pricePerGram * 0.75 * 3.75));
        model.addAttribute("price14k", (int) Math.round(pricePerGram * 0.585 * 3.75));

        List<String> chartLabels = new ArrayList<>();
        List<Integer> chartData = new ArrayList<>();

        for (int i = history.length - 1; i >= 0; i--) {
            chartLabels.add("\"" + history[i].getEffectiveDate() + "\""); // ✅ JS에서 문자열로 인식되도록 쌍따옴표
            chartData.add((int) Math.round(history[i].getPricePerGram() * 3.75));
        }

        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartData", chartData);

        return "gold_price";
    }

}