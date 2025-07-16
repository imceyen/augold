package com.global.augold.detailPage.controller;

import com.global.augold.detailPage.dto.DetailPageDTO;
import com.global.augold.detailPage.service.DetailPageService;
import com.global.augold.member.entity.Customer;
import com.global.augold.product.entity.Product;
import com.global.augold.product.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class DetailPageController {

    private final DetailPageService detailPageService;
    private final ProductRepository productRepository;

    @GetMapping("/product/{id}")
    public String showProductDetail(@PathVariable("id") String productId, Model model, HttpSession session) {

        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser != null) {
            String loginName = loginUser.getCstmName();
            model.addAttribute("loginName", loginName);
        }

        // 1. 상품 정보 조회
        DetailPageDTO dto = detailPageService.getProductById(productId);

        System.out.println("📷 이미지1: " + dto.getImageUrl1());
        System.out.println("📷 이미지2: " + dto.getImageUrl2());
        System.out.println("📷 이미지3: " + dto.getImageUrl3());

        // 2. 옵션 조회 분기 처리
        List<Product> productOptions = new ArrayList<>();

        if ("감사패".equals(dto.getSubCtgr()) || "카네이션기념품".equals(dto.getSubCtgr())) {
            productOptions = List.of();

        } else if ("돌반지".equals(dto.getSubCtgr())) {
            productOptions = productRepository.findAll().stream()
                    .filter(p -> "돌반지".equals(p.getSubCtgr()))
                    .collect(Collectors.toList());

        } else if (dto.getProductGroup() != null && !dto.getProductGroup().isEmpty()) {
            productOptions = productRepository.findByProductGroup(dto.getProductGroup());

        } else {
            productOptions = productRepository.findAll().stream()
                    .filter(p -> p.getCtgrId().equals(dto.getCtgrId()))
                    .filter(p -> p.getProductName() != null && dto.getProductName() != null)
                    .filter(p -> {
                        String currentName = dto.getProductName().replaceAll("\\s*\\d+(\\.\\d+)?g", "")
                                .replaceAll("14K|18K|24K|순금", "").trim();
                        String optionName = p.getProductName().replaceAll("\\s*\\d+(\\.\\d+)?g", "")
                                .replaceAll("14K|18K|24K|순금", "").trim();
                        return currentName.equals(optionName);
                    })
                    .collect(Collectors.toList());
        }

        // 3. DTO 변환
        List<DetailPageDTO> options = productOptions.stream()
                .map(p -> {
                    String cleanName = p.getProductName();
                    if (cleanName != null) {
                        cleanName = cleanName.replaceAll("\\s*\\d+(\\.\\d+)?g", "")
                                .replaceAll("\\s*14K|\\s*18K|\\s*24K|\\s*순금", "")
                                .replaceAll("\\s+", " ").trim();
                    }

                    return DetailPageDTO.builder()
                            .productId(p.getProductId())
                            .productName(cleanName)
                            .finalPrice(p.getFinalPrice())
                            .karatCode(p.getKaratCode())
                            .goldWeight(p.getGoldWeight())
                            .subCtgr(p.getSubCtgr())
                            .ctgrId(p.getCtgrId())
                            .productInventory(p.getProductInventory())
                            .build();
                })
                .collect(Collectors.toList());

        // 4. 중복 제거
        Set<String> seen = new HashSet<>();
        List<DetailPageDTO> deduplicatedOptions = new ArrayList<>();

        for (DetailPageDTO opt : options) {
            String key = opt.getKaratCode() + "-" + opt.getGoldWeight();
            if (!seen.contains(key)) {
                seen.add(key);
                deduplicatedOptions.add(opt);
            }
        }
        options = deduplicatedOptions;

        // 5. 정렬
        options.sort(Comparator.comparingInt(opt -> {
            switch (opt.getKaratCode()) {
                case "14K":
                    return 1;
                case "18K":
                    return 2;
                case "24K":
                    return 3;
                default:
                    return 99;
            }
        }));

        // 6. 골드바 가격 계산 또는 일반 상품 옵션 적용
        if ("CTGR-00002".equals(dto.getCtgrId())) {
            double marketPrice = detailPageService.getLatestGoldPrice();
            double goldPricePerGram = marketPrice * 1.1;

            if (dto.getGoldWeight() != null) {
                double newPrice = dto.getGoldWeight() * goldPricePerGram;
                dto.setFinalPrice(newPrice);
                System.out.println("✅ 골드바 실시간 계산 가격: " + newPrice);
            }

            // ✅ 골드바 재고 정보도 세팅
            dto.setProductInventory(productRepository.findById(productId)
                    .map(Product::getProductInventory)
                    .orElse(0));

        } else {
            DetailPageDTO baseOption = options.stream()
                    .filter(opt -> "14K".equals(opt.getKaratCode()))
                    .findFirst()
                    .orElse(!options.isEmpty() ? options.get(0) : dto);

            dto.setFinalPrice(baseOption.getFinalPrice());
            dto.setKaratCode(baseOption.getKaratCode());

            // ✅ 일반 상품 재고 세팅
            if (dto.getProductInventory() == null && baseOption.getProductInventory() != null) {
                dto.setProductInventory(baseOption.getProductInventory());
            }
        }

        // 7. 모델에 담기
        model.addAttribute("product", dto);
        model.addAttribute("options", options);

        // 8. 카테고리 구분값
        String selectedType;
        switch (dto.getCtgrId()) {
            case "CAT001":
                selectedType = "goldbar";
                break;
            case "CAT002":
                selectedType = "jewelry";
                break;
            case "CAT003":
                selectedType = "gift";
                break;
            default:
                selectedType = "goldbar";
                break;
        }
        model.addAttribute("selectedType", selectedType);

        return "product/detailPage";
    }
}