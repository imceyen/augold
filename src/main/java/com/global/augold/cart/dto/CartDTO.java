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
    private double finalPrice;
    private String imageUrl;
    private String ctgrNm;
    private String karatName;
    private double goldWeight;
    private int quantity = 1;
    private String productGroup;

    // JPQL용 생성자
    public CartDTO(String cartNumber, String productId, LocalDateTime cartDate,
                   String cstmNumber, String productName, Double finalPrice,
                   String imageUrl, String ctgrId, String karatCode, String productGroup, Integer quantity) {
        this.cartNumber = cartNumber;
        this.productId = productId;
        this.cartDate = cartDate;
        this.cstmNumber = cstmNumber;
        this.productName = productName;
        this.finalPrice = finalPrice != null ? finalPrice : 0.0;
        this.imageUrl = imageUrl;
        this.ctgrNm = ctgrId;      // 일단 ID로 표시
        this.karatName = karatCode; // 일단 코드로 표시
        this.goldWeight = 0.0;
        this.productGroup = productGroup;
        this.quantity = quantity != null ? quantity : 1; // ✅ quantity 파라미터 추가!
    }

    // 기본 정보만 있는 생성자
    public CartDTO(String cartNumber, String productId, LocalDateTime cartDate, String cstmNumber){
        this.cartNumber = cartNumber;
        this.cartDate = cartDate;
        this.productId = productId;
        this.cstmNumber = cstmNumber;
    }
}