package com.global.augold.detailPage.controller;

import com.global.augold.detailPage.dto.DetailPageDTO;
import com.global.augold.detailPage.service.DetailPageService;
import com.global.augold.product.entity.Product;
import com.global.augold.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class DetailPageController {

    private final DetailPageService detailPageService;
    private final ProductRepository productRepository;

    @GetMapping("/product/{id}")
    public String showProductDetail(@PathVariable("id") String productId, Model model) {
        // 1. 단일 상품 정보 가져오기
        DetailPageDTO dto = detailPageService.getProductById(productId);


        // 1-1. 골드바일 경우 시세 기반 가격 적용
        if ("0002".equals(dto.getCategoryId())) {
            double marketPrice = detailPageService.getLatestGoldPrice(); // 금 시세 가져오기
            double sellingRate = 1.1;
            double goldPricePerGram = marketPrice * sellingRate;

            if (dto.getGoldWeight() != null) {
                double finalPrice = dto.getGoldWeight() * goldPricePerGram;
                dto.setFinalPrice(finalPrice);
            }
        }

        // 2. productGroup 기준으로 옵션 상품들 가져오기 (null 체크 추가)
        List<Product> productOptions;
        if (dto.getProductGroup() != null && !dto.getProductGroup().isEmpty()) {
            productOptions = productRepository.findByProductGroup(dto.getProductGroup());
        } else {
            // productGroup이 null이면 같은 카테고리의 상품들만 가져오기
            productOptions = productRepository.findAll().stream()
                    .filter(p -> p.getCtgrId().equals(dto.getCategoryId()))
                    .filter(p -> p.getProductName() != null && dto.getProductName() != null)
                    .filter(p -> {
                        // 같은 디자인 패턴의 상품만 필터링
                        String currentName = dto.getProductName().replaceAll("\\s*\\d+(\\.\\d+)?g", "")
                                .replaceAll("14K|18K|24K|순금", "").trim();
                        String optionName = p.getProductName().replaceAll("\\s*\\d+(\\.\\d+)?g", "")
                                .replaceAll("14K|18K|24K|순금", "").trim();
                        return currentName.equals(optionName);
                    })
                    .collect(Collectors.toList());
        }

        // Product를 DetailPageDTO로 변환 (상품명 정리 포함)
        List<DetailPageDTO> options = productOptions.stream()
                .map(product -> {
                    // 상품명에서 순도 정보 제거
                    String cleanProductName = product.getProductName();
                    if (cleanProductName != null) {
                        cleanProductName = cleanProductName.replaceAll("\\s*\\d+(\\.\\d+)?g", "") // 중량 제거
                                .replaceAll("\\s*14K|\\s*18K|\\s*24K|\\s*순금", "") // 순도 제거
                                .replaceAll("\\s+", " ") // 연속된 공백을 하나로
                                .trim();
                    }

                    return DetailPageDTO.builder()
                            .productId(product.getProductId())
                            .productName(cleanProductName)
                            .finalPrice(product.getFinalPrice())
                            .karatCode(product.getKaratCode())
                            .goldWeight(product.getGoldWeight())
                            .subCtgr(product.getSubCtgr())
                            .categoryId(product.getCtgrId())
                            .build();
                })
                .collect(Collectors.toList());

        // 디버깅: 상품 정보 출력
        System.out.println("=== 상세 페이지 디버깅 ===");
        System.out.println("상품 ID: " + dto.getProductId());
        System.out.println("상품명: " + dto.getProductName());
        System.out.println("카테고리: " + dto.getSubCtgr());
        System.out.println("순도: " + dto.getKaratCode());
        System.out.println("중량: " + dto.getGoldWeight());
        System.out.println("가격: " + dto.getFinalPrice());
        System.out.println("상품그룹: " + dto.getProductGroup());

        System.out.println("=== 옵션 상품들 ===");
        options.forEach(option -> {
            System.out.println("옵션 - ID: " + option.getProductId() +
                    ", 순도: " + option.getKaratCode() +
                    ", 중량: " + option.getGoldWeight() +
                    ", 가격: " + option.getFinalPrice());
        });

        // 3. model에 전달
        model.addAttribute("product", dto);
        model.addAttribute("options", options);

        // 카테고리 ID 기반으로 selectedType 결정
        String selectedType;
        switch (dto.getCategoryId()) {
            case "CAT001": selectedType = "goldbar"; break;
            case "CAT002": selectedType = "jewelry"; break;
            case "CAT003": selectedType = "gift"; break;
            default: selectedType = "goldbar"; break;
        }
        model.addAttribute("selectedType", selectedType);

        return "product/detailPage";
    }
}
