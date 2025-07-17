package com.global.augold.detailPage.service;

import com.global.augold.common.util.FileUploadUtil;
import com.global.augold.detailPage.repository.DetailPageRepository;
import com.global.augold.detailPage.entity.ProductDetailImage;
import com.global.augold.detailPage.dto.DetailPageDTO;
import com.global.augold.product.entity.Product;
import com.global.augold.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import com.global.augold.goldPrice.Service.GoldPriceService;
import com.global.augold.goldPrice.dto.GoldPriceDTO;

@Service
@RequiredArgsConstructor
public class DetailPageService {

    private final ProductRepository productRepository;
    private final DetailPageRepository detailImageRepository;
    private final GoldPriceService goldPriceService;

    // 금 시세 가져오기 (기존 메서드)
    public double getLatestGoldPrice() {
        GoldPriceDTO dto = goldPriceService.getTodayGoldPrice();
        if (dto != null) {
            return dto.getPricePerGram();
        } else {
            throw new IllegalStateException("금 시세를 불러오지 못했습니다.");
        }
    }

    // 상품 상세 조회 (기존 메서드)
    public DetailPageDTO getProductById(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));

        List<ProductDetailImage> images = detailImageRepository.findByProductId(productId);

        String imageUrl1 = images.size() > 0 ? images.get(0).getImageUrl() : null;
        String imageUrl2 = images.size() > 1 ? images.get(1).getImageUrl() : null;
        String imageUrl3 = images.size() > 2 ? images.get(2).getImageUrl() : null;

        String cleanProductName = product.getProductName();
        if (cleanProductName != null) {
            cleanProductName = cleanProductName.replaceAll("\\s*\\d+(\\.\\d+)?g", "")
                    .replaceAll("\\s*14K|\\s*18K|\\s*24K|\\s*순금", "")
                    .replaceAll("\\s+", " ")
                    .trim();
        }

        return DetailPageDTO.builder()
                .productId(product.getProductId())
                .productName(cleanProductName)
                .finalPrice(product.getFinalPrice())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())
                .imageUrl1(imageUrl1)
                .imageUrl2(imageUrl2)
                .imageUrl3(imageUrl3)
                .productGroup(product.getProductGroup())
                .karatCode(product.getKaratCode())
                .goldWeight(product.getGoldWeight())
                .subCtgr(product.getSubCtgr())
                .ctgrId(product.getCtgrId())
                .build();
    }

    // 상세 이미지 저장/수정 메서드
    @Transactional
    public void saveDetailImages(DetailPageDTO dto) {
        String productId = dto.getProductId();

        // 기존 이미지 모두 삭제
        detailImageRepository.deleteByProductId(productId);

        // 이미지 최대 3장까지 저장
        if (dto.getImageUrl1() != null && !dto.getImageUrl1().isEmpty()) {
            ProductDetailImage img1 = ProductDetailImage.builder()
                    .productId(productId)
                    .imageUrl(dto.getImageUrl1())
                    .build();
            detailImageRepository.save(img1);
        }
        if (dto.getImageUrl2() != null && !dto.getImageUrl2().isEmpty()) {
            ProductDetailImage img2 = ProductDetailImage.builder()
                    .productId(productId)
                    .imageUrl(dto.getImageUrl2())
                    .build();
            detailImageRepository.save(img2);
        }
        if (dto.getImageUrl3() != null && !dto.getImageUrl3().isEmpty()) {
            ProductDetailImage img3 = ProductDetailImage.builder()
                    .productId(productId)
                    .imageUrl(dto.getImageUrl3())
                    .build();
            detailImageRepository.save(img3);
        }
    }

    // 상세 이미지 업로드 처리
    public String uploadImage(MultipartFile file) throws IOException {
        String uploadDir = "C:/upload/detail";
        return FileUploadUtil.upload(file, uploadDir);
    }
} 