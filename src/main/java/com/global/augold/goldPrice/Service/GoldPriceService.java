package com.global.augold.goldPrice.Service;

import java.util.Comparator;
import java.util.List;

import com.global.augold.goldPrice.dto.GoldPriceDTO;
import com.global.augold.goldPrice.util.ExcelReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoldPriceService {

    public GoldPriceDTO getTodayGoldPrice() {
        List<GoldPriceDTO> list = ExcelReader.readGoldPriceExcel("C:/ncsGlobal/FinalProject/final_goldprice/금시세.xlsx");

        if (list == null || list.isEmpty()) {
            throw new IllegalStateException("금 시세 데이터가 없습니다.");
        }

        GoldPriceDTO latest = list.stream()
                .max(Comparator.comparing(GoldPriceDTO::getEffectiveDate))
                .orElseThrow(() -> new IllegalStateException("유효한 시세가 없습니다."));

        // ✅ 이 줄 추가
        System.out.println("✅ 최신 금 시세: " + latest.getPricePerGram() + " (날짜: " + latest.getEffectiveDate() + ")");

        return latest;
    }



    public GoldPriceDTO[] getGoldPriceHistory() {
        List<GoldPriceDTO> list = ExcelReader.readGoldPriceExcel("C:/ncsGlobal/FinalProject/final_goldprice/금시세.xlsx");
        return list.toArray(new GoldPriceDTO[0]);
    }
}
