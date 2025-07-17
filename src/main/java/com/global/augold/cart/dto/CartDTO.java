package com.global.augold.cart.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class CartDTO {
    private String cartNumber;
    private String productId;
    private LocalDateTime cartDate;
    private String cstmNumber;
    private String productName;
    private BigDecimal finalPrice;
    private String imageUrl;
    private String ctgrNm;
    private String karatName;
    private BigDecimal goldWeight;
    private int quantity = 1;

    // JPQL용 생성자
    public CartDTO(String cartNumber, String productId, LocalDateTime cartDate,
                   String cstmNumber, String productName, Double finalPrice,
                   String imageUrl, String ctgrId, String karatCode) {
        this.cartNumber = cartNumber;
        this.productId = productId;
        this.cartDate = cartDate;
        this.cstmNumber = cstmNumber;
        this.productName = productName;
        this.finalPrice = finalPrice != null ? BigDecimal.valueOf(finalPrice) : BigDecimal.ZERO;
        this.imageUrl = imageUrl;
        this.ctgrNm = ctgrId;      // 일단 ID로 표시
        this.karatName = karatCode; // 일단 코드로 표시
        this.goldWeight = BigDecimal.ZERO; // 나중에 추가
    }

    // 기본 정보만 있는 생성자
    public CartDTO(String cartNumber, String productId, LocalDateTime cartDate, String cstmNumber){
        this.cartNumber = cartNumber;
        this.cartDate = cartDate;
        this.productId = productId;
        this.cstmNumber = cstmNumber;
    }
}