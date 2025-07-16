package com.global.augold.detailPage.service;
import com.global.augold.detailPage.repository.DetailPageRepository;
import com.global.augold.detailPage.entity.ProductDetailImage;
import com.global.augold.detailPage.dto.DetailPageDTO;
import com.global.augold.product.entity.Product;
import com.global.augold.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import com.global.augold.goldPrice.Service.GoldPriceService;
import com.global.augold.goldPrice.dto.GoldPriceDTO;



@Service
@RequiredArgsConstructor
public class DetailPageService {

    private final ProductRepository productRepository;
    private final DetailPageRepository detailImageRepository;
    private final GoldPriceService goldPriceService; // 👈 주입받기

    // 👇 여기에 메서드 추가
    public double getLatestGoldPrice() {
        GoldPriceDTO dto = goldPriceService.getTodayGoldPrice(); // 외부 API에서 금 시세 가져옴

        if (dto != null) {
            return dto.getPricePerGram(); // 여기서 시세(double)를 반환
        } else {
            throw new IllegalStateException("금 시세를 불러오지 못했습니다.");
        }
    }



    public DetailPageDTO getProductById(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));

        List<ProductDetailImage> images = detailImageRepository.findByProductId(productId);

        String imageUrl1 = images.size() > 0 ? images.get(0).getImageUrl() : null;
        String imageUrl2 = images.size() > 1 ? images.get(1).getImageUrl() : null;
        String imageUrl3 = images.size() > 2 ? images.get(2).getImageUrl() : null;

        // 상품명에서 순도 정보 제거
        String cleanProductName = product.getProductName();
        if (cleanProductName != null) {
            cleanProductName = cleanProductName.replaceAll("\\s*\\d+(\\.\\d+)?g", "") // 중량 제거
                    .replaceAll("\\s*14K|\\s*18K|\\s*24K|\\s*순금", "") // 순도 제거
                    .replaceAll("\\s+", " ") // 연속된 공백을 하나로
                    .trim();
        }

        DetailPageDTO dto = DetailPageDTO.builder()
                .productId(product.getProductId())
                .productName(cleanProductName)
                .finalPrice(product.getFinalPrice())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())  // 메인 이미지
                .imageUrl1(imageUrl1)
                .imageUrl2(imageUrl2)
                .imageUrl3(imageUrl3)
                .productGroup(product.getProductGroup())
                .karatCode(product.getKaratCode())
                .goldWeight(product.getGoldWeight())
                .subCtgr(product.getSubCtgr())
                .ctgrId(product.getCtgrId())
                .build();

        System.out.println("imageUrl1 = " + imageUrl1);
        return dto;
    }


}

