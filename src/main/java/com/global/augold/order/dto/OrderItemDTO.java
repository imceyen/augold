// ===============================
// OrderItemDTO.java - 주문 상품 DTO
// ===============================

package com.global.augold.order.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDTO {

    private String orderItemId;     // 주문상세ID
    private String orderNumber;     // 주문번호
    private String productId;       // 상품번호
    private String productName;     // 상품명 (Product에서 조회)
    private String karatCode;       // 금 순도 (Product에서 조회)
    private String imageUrl;        // 상품 이미지 (Product에서 조회)
    private Integer quantity;       // 수량
    private BigDecimal unitPrice;   // 단가
    private BigDecimal finalAmount; // 최종금액
    private Double goldWeight;      // 금 중량 (Product에서 조회)
    private String productGroup;    // 상품그룹 (Product에서 조회)

    /**
     * 총 가격 반환 (finalAmount와 동일하지만 명시적)
     */
    public BigDecimal getTotalPrice() {
        return finalAmount != null ? finalAmount : BigDecimal.ZERO;
    }

    /**
     * 포맷된 가격 문자열 반환
     */
    public String getFormattedUnitPrice() {
        if (unitPrice == null) return "₩0";
        return String.format("₩%,d", unitPrice.intValue());
    }

    /**
     * 포맷된 총 금액 문자열 반환
     */
    public String getFormattedTotalPrice() {
        if (finalAmount == null) return "₩0";
        return String.format("₩%,d", finalAmount.intValue());
    }

    /**
     * 금 순도 표시명 반환
     */
    public String getKaratDisplayName() {
        if (karatCode == null) return "";

        switch (karatCode.toUpperCase()) {
            case "24K": return "24K (순금)";
            case "22K": return "22K";
            case "18K": return "18K";
            case "14K": return "14K";
            default: return karatCode;
        }
    }

    /**
     * 상품 요약 정보
     */
    public String getProductSummary() {
        StringBuilder summary = new StringBuilder();
        if (productName != null) {
            summary.append(productName);
        }
        if (karatCode != null) {
            summary.append(" (").append(karatCode).append(")");
        }
        if (quantity != null && quantity > 1) {
            summary.append(" ").append(quantity).append("개");
        }
        return summary.toString();
    }

    /**
     * 유효성 검증
     */
    public boolean isValid() {
        return orderNumber != null && !orderNumber.trim().isEmpty()
                && productId != null && !productId.trim().isEmpty()
                && quantity != null && quantity > 0
                && unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) > 0;
    }
}