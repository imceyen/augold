package com.global.augold.categoryPage.controller;

import com.global.augold.mainPage.dto.MainPageInfoDTO;
import com.global.augold.mainPage.service.MainPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class CategoryPageController {

    private final MainPageService mainPageService;

    @GetMapping("/category")
    public String showCategoryPage(Model model) {
        List<MainPageInfoDTO> allProducts = mainPageService.getAllProducts();

        // 골드바 (37.5g 제외)
        List<MainPageInfoDTO> goldbars = allProducts.stream()
                .filter(p -> "0002".equals(p.getCategoryId()))
                .filter(p -> p.getProductName() == null || !p.getProductName().contains("37.5"))
                .collect(Collectors.toList());

        // 주얼리 중복 제거
        List<MainPageInfoDTO> necklaces = filterJewelryUniqueDesign(allProducts, "목걸이");
        List<MainPageInfoDTO> earrings  = filterJewelryUniqueDesign(allProducts, "귀걸이");
        List<MainPageInfoDTO> rings     = filterJewelryUniqueDesign(allProducts, "반지");

        // 기념품 전체
        List<MainPageInfoDTO> allGifts = allProducts.stream()
                .filter(p -> "0003".equals(p.getCategoryId()))
                .collect(Collectors.toList());

        // 포카락/첫돌 필터링 후 중복 제거
        List<MainPageInfoDTO> specialGifts = allGifts.stream()
                .filter(p -> {
                    String name = p.getProductName();
                    return name != null && (
                            name.contains("포카락") || name.contains("돌반지") || name.contains("첫돌"));
                })
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                p -> extractDesignKey(p.getProductName()),
                                p -> {
                                    // 상품명에서 g 제거
                                    p.setProductName(cleanProductName(p.getProductName()));
                                    return p;
                                },
                                (existing, replacement) -> existing
                        ),
                        map -> new ArrayList<>(map.values())
                ));

        // 그 외 기념품
        List<MainPageInfoDTO> otherGifts = allGifts.stream()
                .filter(p -> {
                    String name = p.getProductName();
                    return name == null || (
                            !name.contains("포카락") && !name.contains("돌반지") && !name.contains("첫돌"));
                })
                .collect(Collectors.toList());

        // 기념품 전체 결합
        List<MainPageInfoDTO> gifts = new ArrayList<>();
        gifts.addAll(specialGifts);
        gifts.addAll(otherGifts);

        // 모델 등록
        model.addAttribute("goldbars", goldbars);
        model.addAttribute("necklaces", necklaces);
        model.addAttribute("earrings", earrings);
        model.addAttribute("rings", rings);
        model.addAttribute("gifts", gifts);

        return "product/Category_Product";
    }

    // 주얼리 중복 제거
    private List<MainPageInfoDTO> filterJewelryUniqueDesign(List<MainPageInfoDTO> products, String keyword) {
        return products.stream()
                .filter(p -> "0001".equals(p.getCategoryId()))
                .filter(p -> p.getProductName() != null && p.getProductName().contains(keyword))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                p -> extractDesignKey(p.getProductName()),
                                p -> {
                                    p.setProductName(cleanProductName(p.getProductName()));
                                    return p;
                                },
                                (existing, replacement) -> existing
                        ),
                        map -> new ArrayList<>(map.values())
                ));
    }

    // 중복 제거 키 (디자인 기준)
    private String extractDesignKey(String name) {
        if (name == null) return "";
        return name.replaceAll("\\s*\\d+(\\.\\d+)?g", "")
                .replaceAll("14K|18K|순금", "")
                .replaceAll("\\s+", "")
                .trim();
    }

    // 상품명 정리 (g 제거용)
    private String cleanProductName(String name) {
        if (name == null) return "";
        return name.replaceAll("\\s*\\d+(\\.\\d+)?g", "").trim();
    }
}
