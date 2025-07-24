package com.global.augold.product.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 상품 정보를 담는 DTO 클래스
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {
    private String productId;     // 상품 ID (PK)
    private String karatCode;     // 금 함량 코드 (예: 14K, 18K)
    private String ctgrId;        // 카테고리 ID (FK)
    private String productName;   // 상품명
    private Double basePrice;     // 기본 가격
    private Double goldWeight;    // 금 무게 (g)
    private Double finalPrice;    // 최종 가격
    private String imageUrl;      // 대표 이미지 URL
    private String description;   // 상품 설명
    private String subCtgr;       // 서브 카테고리 (ex. 귀걸이, 반지 등)

    private String imageUrl1; // 상세 이미지 1
    private String imageUrl2; // 상세 이미지 2
    private String imageUrl3; // 상세 이미지 3

    private MultipartFile imageFile;     // 대표 이미지 파일
    private MultipartFile detailImage1;  // 상세 이미지 1
    private MultipartFile detailImage2;  // 상세 이미지 2
    private MultipartFile detailImage3;  // 상세 이미지 3


}
