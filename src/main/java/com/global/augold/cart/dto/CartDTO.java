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

    // JPQL용 생성자
    public CartDTO(String cartNumber, String productId, LocalDateTime cartDate,
                   String cstmNumber, String productName, BigDecimal finalPrice,
                   String imageUrl, String ctgrNm, String karatName, BigDecimal goldWeight) {
        this.cartNumber = cartNumber;
        this.productId = productId;
        this.cartDate = cartDate;
        this.cstmNumber = cstmNumber;
        this.productName = productName;
        this.finalPrice = finalPrice;
        this.imageUrl = imageUrl;
        this.ctgrNm = ctgrNm;
        this.karatName = karatName;
        this.goldWeight = goldWeight;
    }

    // 기본 정보만 있는 생성자
    public CartDTO(String cartNumber, String productId, LocalDateTime cartDate, String cstmNumber){
        this.cartNumber = cartNumber;
        this.cartDate = cartDate;
        this.productId = productId;
        this.cstmNumber = cstmNumber;
    }
}