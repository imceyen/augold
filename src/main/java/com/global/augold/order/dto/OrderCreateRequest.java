// ===============================
// OrderCreateRequest.java - 주문 생성 요청 DTO
// ===============================

package com.global.augold.order.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest {

    private String cstmNumber;      // 고객번호 (세션에서 설정)
    private String deliveryAddr;    // 배송주소
    private String deliveryPhone;   // 배송 연락처
    private String orderMemo;       // 주문 메모 (선택사항)

    // 생성자 (필수 필드만)
    public OrderCreateRequest(String deliveryAddr, String deliveryPhone) {
        this.deliveryAddr = deliveryAddr;
        this.deliveryPhone = deliveryPhone;
    }

    /**
     * 유효성 검증
     */
    public boolean isValid() {
        return deliveryAddr != null && !deliveryAddr.trim().isEmpty()
                && deliveryPhone != null && !deliveryPhone.trim().isEmpty();
    }
}