package com.global.augold.detailPage.service;

import com.global.augold.detailPage.dto.DetailPageDTO;
import com.global.augold.detailPage.entity.ProductDetailImage;
import com.global.augold.detailPage.repository.DetailPageRepository;
import com.global.augold.product.entity.Product;
import com.global.augold.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DetailPageService {

    private final ProductRepository productRepository;
    private final DetailPageRepository detailImageRepository;

    public DetailPageDTO getProductById(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));

        List<ProductDetailImage> images = detailImageRepository.findByProductId(productId);

        String imageUrl1 = images.size() > 0 ? images.get(0).getImageUrl() : null;
        String imageUrl2 = images.size() > 1 ? images.get(1).getImageUrl() : null;
        String imageUrl3 = images.size() > 2 ? images.get(2).getImageUrl() : null;

        DetailPageDTO dto = DetailPageDTO.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .finalPrice(product.getFinalPrice())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())  // 메인 이미지
                .imageUrl1(imageUrl1)
                .imageUrl2(imageUrl2)
                .imageUrl3(imageUrl3)
                .build();
        return dto;
    }
}

