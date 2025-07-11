package com.global.augold.detailPage.controller;

import com.global.augold.detailPage.dto.DetailPageDTO;
import com.global.augold.detailPage.service.DetailPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class DetailPageController {

    private final DetailPageService detailPageService;

    @GetMapping("/product/{id}")
    public String showProductDetail(@PathVariable("id") String productId, Model model) {
        // ❗서비스에서 이미지 처리된 DTO를 받아옴
        DetailPageDTO dto = detailPageService.getProductById(productId);

        model.addAttribute("product", dto);
        return "product/detailPage";
    }
}
