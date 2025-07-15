package com.global.augold.goldPrice.Service;

// GoldPriceService.java

import com.global.augold.goldPrice.dto.GoldPriceDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
@RequiredArgsConstructor
public class GoldPriceService {

    public GoldPriceDTO getTodayGoldPrice() {
        RestTemplate restTemplate = new RestTemplate();
        String apiUrl = "http://localhost:5000/api/gold-price";

        try {
            String json = restTemplate.getForObject(apiUrl, String.class);
            System.out.println("📥 받아온 JSON 문자열: " + json);

            // 매핑 테스트
            ObjectMapper mapper = new ObjectMapper();
            GoldPriceDTO dto = mapper.readValue(json, GoldPriceDTO.class);

            System.out.println("✅ 매핑 성공: " + dto.getEffectiveDate() + ", " + dto.getPricePerGram());
            return dto;

        } catch (Exception e) {
            System.out.println("❌ 매핑 실패: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }



    public GoldPriceDTO[] getGoldPriceHistory() {
        RestTemplate restTemplate = new RestTemplate();
        String apiUrl = "http://localhost:5000/api/goldprice/history";  // 정확히 일치해야 함
        System.out.println("📡 요청 URL: " + apiUrl);
        return restTemplate.getForObject(apiUrl, GoldPriceDTO[].class);
    }

}



