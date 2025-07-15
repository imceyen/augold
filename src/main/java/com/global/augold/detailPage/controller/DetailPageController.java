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

        // 1-1. 골드바일 경우 실시간 시세 반영
        if ("0002".equals(dto.getCategoryId())) {
            double marketPrice = detailPageService.getLatestGoldPrice();
            double goldPricePerGram = marketPrice * 1.1;
            if (dto.getGoldWeight() != null) {
                dto.setFinalPrice(dto.getGoldWeight() * goldPricePerGram);
            }
        }

        // 2. 옵션 조회 분기 처리
        // 2. 옵션 조회 분기 처리
        List<Product> productOptions = new ArrayList<>();

        if ("감사패".equals(dto.getSubCtgr()) || "카네이션기념품".equals(dto.getSubCtgr())) {
            // 옵션 없음 (select 박스 숨김용)
            productOptions = List.of();

        } else if ("돌반지".equals(dto.getSubCtgr())) {
            // 중량 옵션 존재
            productOptions = productRepository.findAll().stream()
                    .filter(p -> "돌반지".equals(p.getSubCtgr()))
                    .collect(Collectors.toList());

        } else if (dto.getProductGroup() != null && !dto.getProductGroup().isEmpty()) {
            // productGroup이 있는 경우
            productOptions = productRepository.findByProductGroup(dto.getProductGroup());

        } else {
            // fallback: 이름 유사성 기반 옵션 구성
            productOptions = productRepository.findAll().stream()
                    .filter(p -> p.getCtgrId().equals(dto.getCategoryId()))
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


        // 3. 옵션 DTO로 변환
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
                            .categoryId(p.getCtgrId())
                            .build();
                })
                .collect(Collectors.toList());

        // 4. 옵션 정렬 (순도)
        // 4. 옵션 정렬 후 추가
        options.sort(Comparator.comparingInt(opt -> {
            switch (opt.getKaratCode()) {
                case "14K": return 1;
                case "18K": return 2;
                case "24K": return 3;
                default: return 99;
            }
        }));

        // ✅ 완전히 동일한 goldWeight + karatCode 조합만 제거
        Set<String> seen = new HashSet<>();
        List<DetailPageDTO> deduplicatedOptions = new ArrayList<>();

        for (DetailPageDTO opt : options) {
            String key = opt.getKaratCode() + "-" + opt.getGoldWeight(); // 예: "14K-1.875"
            if (!seen.contains(key)) {
                seen.add(key);
                deduplicatedOptions.add(opt);
            }
        }

        options = deduplicatedOptions;


        // 5. 옵션 중 기본값 적용 (기본: 14K → 없으면 첫 번째)
        DetailPageDTO baseOption = options.stream()
                .filter(opt -> "14K".equals(opt.getKaratCode()))
                .findFirst()
                .orElse(!options.isEmpty() ? options.get(0) : dto);

        dto.setFinalPrice(baseOption.getFinalPrice());
        dto.setKaratCode(baseOption.getKaratCode());

        // 6. 모델에 전달
        model.addAttribute("product", dto);
        model.addAttribute("options", options);

        // 7. 카테고리 기반 타입 설정
        String selectedType;
        switch (dto.getCategoryId()) {
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
