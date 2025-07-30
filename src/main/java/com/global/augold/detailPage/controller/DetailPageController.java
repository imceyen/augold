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
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequiredArgsConstructor
public class DetailPageController {

    private final DetailPageService detailPageService;
    private final ProductRepository productRepository;

    @GetMapping("/product/{id}")
    public String showProductDetail(@PathVariable("id") String productId, Model model, HttpSession session) {

        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser != null) {
            model.addAttribute("loginName", loginUser.getCstmName());
        }

        DetailPageDTO dto = detailPageService.getProductById(productId);

        System.out.println("📷 이미지1: " + dto.getImageUrl1());
        System.out.println("📷 이미지2: " + dto.getImageUrl2());
        System.out.println("📷 이미지3: " + dto.getImageUrl3());

        List<Product> productOptions = new ArrayList<>();

        if ("감사패".equals(dto.getSubCtgr()) || "카네이션기념품".equals(dto.getSubCtgr())) {
            productOptions = List.of();

        } else if ("돌반지".equals(dto.getSubCtgr())) {
            // 🔥 현재 상품명에서 중량 부분 제거
            String baseProductName = dto.getProductName()
                    .replaceAll("\\s*\\d+(\\.\\d+)?g", "") // 중량 제거
                    .replaceAll("\\s+", " ").trim();

            productOptions = productRepository.findAll().stream()
                    .filter(p -> "돌반지".equals(p.getSubCtgr()))
                    .filter(p -> {
                        String optionBaseName = p.getProductName()
                                .replaceAll("\\s*\\d+(\\.\\d+)?g", "") // 중량 제거
                                .replaceAll("\\s+", " ").trim();
                        return baseProductName.equals(optionBaseName); // 같은 제품명만
                    })
                    .toList();

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
                    .toList();
        }

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
                            .description(p.getDescription())
                            .build();
                })
                .toList();

        Set<String> seenKarats = new HashSet<>(); // 순도(Karat)만 추적하기 위한 Set
        List<DetailPageDTO> deduplicatedOptions = new ArrayList<>();

        if ("돌반지".equals(dto.getSubCtgr())) {
            Set<String> seenProductIds = new HashSet<>(); // 🔥 productId 기준으로 변경

            for (DetailPageDTO opt : options) {
                String key = opt.getProductId(); // 🔥 productId 사용

                if (key != null && !seenProductIds.contains(key)) {
                    seenProductIds.add(key);
                    deduplicatedOptions.add(opt);
                }
            }
        } else {
            // 🔥 기존 for문 그대로
            for (DetailPageDTO opt : options) {
                String key = opt.getKaratCode();

                if (key != null && !seenKarats.contains(key)) {
                    seenKarats.add(key);
                    deduplicatedOptions.add(opt);
                }
            }
        }


// 중복이 제거된 리스트로 options 변수를 교체합니다.
        options = deduplicatedOptions;


        options.sort(Comparator.comparingInt(opt -> switch (opt.getKaratCode()) {
            case "14K" -> 1;
            case "18K" -> 2;
            case "24K" -> 3;
            default -> 99;
        }));


        if ("CTGR-00002".equals(dto.getCtgrId())) {
            // 🔥 골드바: 스케줄러가 업데이트한 DB 값 그대로 사용
            // dto.setFinalPrice()는 호출하지 않음 (이미 DB에서 올바른 값 가져옴)

            // 재고 정보만 업데이트
            dto.setProductInventory(productRepository.findById(productId)
                    .map(Product::getProductInventory)
                    .orElse(0));

        } else {
            // 🔥 주얼리: 기존 로직 (14K 기본으로 옵션에서 찾기)
            DetailPageDTO baseOption = options.stream()
                    .filter(opt -> "14K".equals(opt.getKaratCode()))
                    .findFirst()
                    .orElse(!options.isEmpty() ? options.get(0) : dto);



            if (dto.getProductInventory() == null && baseOption.getProductInventory() != null) {
                dto.setProductInventory(baseOption.getProductInventory());
            }
        }

        model.addAttribute("product", dto);
        model.addAttribute("options", options);

        String selectedType = switch (dto.getCtgrId()) {
            case "CAT001" -> "goldbar";
            case "CAT002" -> "jewelry";
            case "CAT003" -> "gift";
            default -> "goldbar";
        };

        model.addAttribute("selectedType", selectedType);

        return "product/detailPage";
    }

}

