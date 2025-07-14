package com.global.augold.mainPage.service;

import com.global.augold.mainPage.dto.MainPageInfoDTO;
import com.global.augold.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MainPageService {

    private final ProductRepository productRepository;

    public MainPageService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<MainPageInfoDTO> getMainPageProducts() {
        // ✅ 실제 DB에 존재하는 PRODUCT_ID 기준
        List<String> ids = List.of("G0005", "S0005", "N0009", "S0008");

        return productRepository.findAllById(ids).stream()
                .map(p -> {
                    MainPageInfoDTO dto = new MainPageInfoDTO();
                    dto.setProductId(p.getProductId());
                    dto.setKaratCode(p.getKaratCode());
                    dto.setCategoryId(p.getCtgrId());
                    dto.setProductName(p.getProductName());
                    dto.setFinalPrice(p.getFinalPrice());
                    dto.setImageUrl(p.getImageUrl());
                    dto.setDescription(p.getDescription());
                    dto.setDescription(p.getSubCtgr());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public List<MainPageInfoDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(p -> {

                    System.out.println("💬 category=" + p.getCtgrId());


                    MainPageInfoDTO dto = new MainPageInfoDTO();
                    dto.setProductId(p.getProductId());
                    dto.setKaratCode(p.getKaratCode());
                    dto.setCategoryId(p.getCtgrId());
                    dto.setProductName(p.getProductName());
                    dto.setFinalPrice(p.getFinalPrice());
                    dto.setImageUrl(p.getImageUrl());
                    dto.setDescription(p.getDescription());
                    dto.setDescription(p.getSubCtgr());
                    return dto;
                })
                .collect(Collectors.toList());
    }

}
