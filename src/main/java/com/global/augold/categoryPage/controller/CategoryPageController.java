package com.global.augold.categoryPage.controller;

import com.global.augold.mainPage.dto.MainPageInfoDTO;
import com.global.augold.mainPage.service.MainPageService;
import com.global.augold.member.entity.Customer;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class CategoryPageController {

    private final MainPageService mainPageService;

    @GetMapping("/category")
    public String showCategoryPage(
            @RequestParam(name = "type", required = false, defaultValue = "goldbar") String type,
            Model model,
            HttpSession session) {

        // 로그인 이름을 항상 모델에 담아줌 (null 허용)
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        String loginName = (loginUser != null) ? loginUser.getCstmName() : null;
        model.addAttribute("loginName", loginName);

        List<MainPageInfoDTO> allProducts = mainPageService.getAllProducts();

        MainPageInfoDTO s00005 = allProducts.stream()
                .filter(p -> "PROD-S00005".equals(p.getProductId()))
                .findFirst()
                .orElse(null);

        // 교체
        allProducts = allProducts.stream()
                .map(product -> {
                    if ("PROD-S00004".equals(product.getProductId()) && s00005 != null) {
                        return s00005;
                    }
                    return product;
                })
                .collect(Collectors.toList());

        // 골드바 (37.5g 제외)
        List<MainPageInfoDTO> goldbars = allProducts.stream()
                .filter(p -> "CTGR-00002".equals(p.getCtgrId()))
                .filter(p -> p.getProductName() == null || !p.getProductName().contains("37.5"))
                .collect(Collectors.toList());

        // 골드 시세 적용 (10% 인상)
        double marketPrice = mainPageService.getLatestGoldPrice(); // 예: 148200
        double sellingRate = 1.1;
        double goldPricePerGram = marketPrice * sellingRate;
        System.out.println("💰 적용된 판매용 시세 (1g당): " + goldPricePerGram);

        // 골드바 가격 계산 적용
        goldbars.forEach(p -> {
            if (p.getGoldWeight() != null) {
                double finalPrice = p.getGoldWeight() * goldPricePerGram;
                p.setFinalPrice(finalPrice);
            }
        });

        goldbars.forEach(p -> System.out.println("▶ 골드바 상품명: " + p.getProductName()));

        // 주얼리 중복 제거
        List<MainPageInfoDTO> necklaces = filterJewelryUniqueDesign(allProducts, "목걸이");
        List<MainPageInfoDTO> earrings  = filterJewelryUniqueDesign(allProducts, "귀걸이");
        List<MainPageInfoDTO> rings     = filterJewelryUniqueDesign(allProducts, "반지");

        // 기념품 전체
        List<MainPageInfoDTO> allGifts = allProducts.stream()
                .filter(p -> "CTGR-00003".equals(p.getCtgrId()))
                .collect(Collectors.toList());

        // 포카락/첫돌 필터링 후 중복 제거 + 기본 옵션 가격 선택 (1.875g)
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
                                    p.setProductName(cleanProductName(p.getProductName()));
                                    return p;
                                },
                                (existing, replacement) -> {
                                    if (isDefaultGiftOption(existing)) {
                                        return existing;
                                    } else if (isDefaultGiftOption(replacement)) {
                                        return replacement;
                                    }
                                    if (existing.getGoldWeight() == 1.875) {
                                        return existing;
                                    } else if (replacement.getGoldWeight() == 1.875) {
                                        return replacement;
                                    }
                                    return existing;
                                }
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

        switch (type.toLowerCase()) {
            case "goldbar":
                model.addAttribute("goldbars", goldbars);
                break;
            case "jewelry":
                model.addAttribute("necklaces", necklaces);
                model.addAttribute("earrings", earrings);
                model.addAttribute("rings", rings);
                break;
            case "gift":
                model.addAttribute("gifts", gifts);
                break;
        }
        model.addAttribute("selectedType", type);

        System.out.println("=== 카테고리 페이지 디버깅 ===");
        System.out.println("골드바 개수: " + goldbars.size());
        goldbars.forEach(p -> System.out.println("골드바 - " + p.getProductName() + ": " + p.getFinalPrice()));

        System.out.println("목걸이 개수: " + necklaces.size());
        necklaces.forEach(p -> System.out.println("목걸이 - " + p.getProductName() + " (" + p.getKaratCode() + " " + p.getGoldWeight() + "g): " + p.getFinalPrice()));

        System.out.println("귀걸이 개수: " + earrings.size());
        earrings.forEach(p -> System.out.println("귀걸이 - " + p.getProductName() + " (" + p.getKaratCode() + " " + p.getGoldWeight() + "g): " + p.getFinalPrice()));

        System.out.println("반지 개수: " + rings.size());
        rings.forEach(p -> System.out.println("반지 - " + p.getProductName() + " (" + p.getKaratCode() + " " + p.getGoldWeight() + "g): " + p.getFinalPrice()));

        System.out.println("기념품 개수: " + gifts.size());
        gifts.forEach(p -> System.out.println("기념품 - " + p.getProductName() + " (" + p.getKaratCode() + " " + p.getGoldWeight() + "g): " + p.getFinalPrice()));

        model.addAttribute("goldbars", goldbars);
        model.addAttribute("necklaces", necklaces);
        model.addAttribute("earrings", earrings);
        model.addAttribute("rings", rings);
        model.addAttribute("gifts", gifts);

        return "product/Category_Product";
    }

    private List<MainPageInfoDTO> filterJewelryUniqueDesign(List<MainPageInfoDTO> products, String keyword) {
        return products.stream()
                .filter(p -> "CTGR-00001".equals(p.getCtgrId()))
                .filter(p -> p.getProductName() != null && p.getProductName().contains(keyword))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                p -> extractDesignKey(p.getProductName()),
                                p -> {
                                    p.setProductName(cleanProductName(p.getProductName()));
                                    return p;
                                },
                                (existing, replacement) -> {
                                    if (isDefaultJewelryOption(existing)) {
                                        return existing;
                                    } else if (isDefaultJewelryOption(replacement)) {
                                        return replacement;
                                    }
                                    if ("14K".equals(existing.getKaratCode())) {
                                        return existing;
                                    } else if ("14K".equals(replacement.getKaratCode())) {
                                        return replacement;
                                    }
                                    return existing;
                                }
                        ),
                        map -> new ArrayList<>(map.values())
                ));
    }

    private boolean isDefaultJewelryOption(MainPageInfoDTO product) {
        return "14K".equals(product.getKaratCode());
    }

    private boolean isDefaultGiftOption(MainPageInfoDTO product) {
        return product.getGoldWeight() == 1.875;
    }

    private String extractDesignKey(String name) {
        if (name == null) return "";
        return name.replaceAll("\\s*\\d+(\\.\\d+)?g", "")
                .replaceAll("14K|18K|순금", "")
                .replaceAll("\\s+", "")
                .trim();
    }

    private String cleanProductName(String name) {
        if (name == null) return "";
        return name.replaceAll("\\s*\\d+(\\.\\d+)?g", "")
                .replaceAll("\\s*14K|\\s*18K|\\s*24K|\\s*순금", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
