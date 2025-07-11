//package com.global.augold.goldPrice.Service;
//
//// GoldPriceService.java
//
//import com.global.augold.goldPrice.dto.GoldPriceDTO;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//@Service
//@RequiredArgsConstructor
//public class GoldPriceService {
//
//    public GoldPriceDTO getTodayGoldPrice() {
//        RestTemplate restTemplate = new RestTemplate();
//        String apiUrl = "http://localhost:5001/api/gold-price"; // 실제 Flask API 주소로 변경
//        return restTemplate.getForObject(apiUrl, GoldPriceDTO.class);
//    }
//
//    public GoldPriceDTO[] getGoldPriceHistory() {
//        RestTemplate restTemplate = new RestTemplate();
//        String apiUrl = "http://localhost:5001/api/goldprice/history";
//        return restTemplate.getForObject(apiUrl, GoldPriceDTO[].class);
//    }
//}
//
//
//
