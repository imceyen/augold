package com.global.augold.mainPage.service;

import com.global.augold.mainPage.dto.MainPageInfoDTO;
import com.global.augold.product.entity.Product;
import com.global.augold.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import com.global.augold.goldPrice.Service.GoldPriceService;
import com.global.augold.goldPrice.dto.GoldPriceDTO;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MainPageService {

    private final ProductRepository productRepository;
    private final GoldPriceService goldPriceService;

    public MainPageService(ProductRepository productRepository, GoldPriceService goldPriceService) {
        this.productRepository = productRepository;
        this.goldPriceService = goldPriceService;
    }

    public double getLatestGoldPrice() {
        GoldPriceDTO dto = goldPriceService.getTodayGoldPrice();
        if (dto != null) {
            return dto.getPricePerGram();
        } else {
            throw new IllegalStateException("금 시세를 불러오지 못했습니다.");
        }
    }


    public List<MainPageInfoDTO> getMainPageProducts() {
        List<String> ids = List.of("PROD-G00005", "PROD-S00005", "PROD-N00009", "PROD-S00008");

        return productRepository.findAllById(ids).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<MainPageInfoDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(p -> {
                    System.out.println("💬 category=" + p.getCtgrId());
                    return convertToDTO(p);
                })
                .collect(Collectors.toList());
    }



    private MainPageInfoDTO convertToDTO(Product p) {
        MainPageInfoDTO dto = new MainPageInfoDTO();
        dto.setProductId(p.getProductId());
        dto.setKaratCode(p.getKaratCode());
        dto.setGoldWeight(p.getGoldWeight());
        dto.setCtgrId(p.getCtgrId());
        dto.setProductName(p.getProductName());
        dto.setFinalPrice(p.getFinalPrice());
        dto.setImageUrl(p.getImageUrl());
        dto.setDescription(p.getDescription());
        return dto;
    }
}
