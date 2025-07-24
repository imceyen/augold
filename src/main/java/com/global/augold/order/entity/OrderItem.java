package com.global.augold.order.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "ORDER_ITEM") // 기존 테이블명과 동일
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @Column(name = "ORDER_ITEM_ID") // 기존 컬럼명과 동일
    private String orderItemId; // 주문상세ID (PK) - DB 트리거에서 자동 생성

    @Column(name = "ORDER_NUMBER", nullable = false)
    private String orderNumber; // 주문번호 - DB에서 생성된 값 저장

    @Column(name = "PRODUCT_ID", nullable = false)
    private String productId; // 상품번호

    @Column(name = "QUANTITY", nullable = false)
    private Integer quantity; // 수량

    @Column(name = "UNIT_PRICE", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice; // 단가

    @Column(name = "FINAL_AMOUNT", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalAmount; // 최종금액 (단가 × 수량)


    @Transient
    private String productName;  // 상품명

    @Transient
    private String imageUrl;     // 상품 이미지 URL


    // OrderItem Order 다 대 일 관계
    @ManyToOne(fetch = FetchType.LAZY) // 여러개의 OrderItem 이 한개의 Order에 속한다 라는 뜻.
    // LAZY의 역할 :  Order 조회할 때는 OrderItem 안 가져옴 (1단계), order.getOrderItems() 호출할 때만 OrderItem 조회 (2단계)
    @JoinColumn(name = "ORDER_NUMBER", insertable = false, updatable = false)
    @JsonBackReference  // 순환참조 방지
    private Order order; // 현재 테이블(ORDER_ITEM)의 ORDER_NUMBER 컬럼으로 Order 테이블과 조인한다


    // 비즈니스 메서드

    //  엔티티 생성 시 기본값 설정

    @PrePersist
    protected void onCreate() {


        if (quantity == null) {
            quantity = 1; // 0개면 아예 안쓰니 상관없음
        }
        // 최종금액 자동 계산
        calculateFinalAmount();
    }


    //  엔티티 수정 시 최종금액 재계산

    @PreUpdate
    protected void onUpdate() {
        calculateFinalAmount();
    }


    //  최종금액 계산 (단가 × 수량)

    public void calculateFinalAmount() {
        if (unitPrice != null && quantity != null) {
            this.finalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }


     // 생성자 - Cart에서 OrderItem으로 변환할 때 사용
    public OrderItem(String orderNumber, String productId, Integer quantity, BigDecimal unitPrice) {
        this.orderNumber = orderNumber;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        // orderItemId는 DB 트리거에서 자동 생성됨
        calculateFinalAmount();
    }


    //  수량 변경 메서드

    public void updateQuantity(Integer newQuantity) {
        if (newQuantity != null && newQuantity > 0) {
            this.quantity = newQuantity;
            calculateFinalAmount();
        }
    }


    //  단가 변경 메서드

    public void updateUnitPrice(BigDecimal newUnitPrice) {
        if (newUnitPrice != null && newUnitPrice.compareTo(BigDecimal.ZERO) > 0) {
            this.unitPrice = newUnitPrice;
            calculateFinalAmount();
        }
    }


    //  상품 총 가격 반환 (finalAmount와 동일하지만 명시적)

    public BigDecimal getTotalPrice() {
        return finalAmount != null ? finalAmount : BigDecimal.ZERO;
    }


    //  CartDTO에서 OrderItem으로 변환하는 정적 메서드

    public static OrderItem fromCart(String orderNumber, String productId, Integer quantity, BigDecimal unitPrice) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderNumber(orderNumber);
        orderItem.setProductId(productId);
        orderItem.setQuantity(quantity);
        orderItem.setUnitPrice(unitPrice);
        // orderItemId는 DB 트리거에서 자동 생성됨
        orderItem.calculateFinalAmount();
        return orderItem;
    }


     // 재고 검증을 위한 정보 제공

    public boolean isValidForStock(int availableStock) {
        return quantity != null && quantity <= availableStock;
    }


     // 재고 부족 시 수량 조정

    public int adjustQuantityForStock(int availableStock) {
        if (quantity == null || availableStock <= 0) {
            return 0; // 주문 불가
        }

        if (quantity > availableStock) {
            int originalQuantity = quantity;
            this.quantity = availableStock;
            calculateFinalAmount();
            return originalQuantity - availableStock; // 장바구니에 남길 수량
        }

        return 0; // 조정 불필요
    }


    //  주문 아이템 유효성 검증

    public boolean isValid() {
        return orderNumber != null && !orderNumber.trim().isEmpty()
                && productId != null && !productId.trim().isEmpty()
                && quantity != null && quantity > 0
                && unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) > 0;
    }


     //  주문상품 정보 요약 문자열

    @Override
    public String toString() {
        return String.format("OrderItem{orderItemId='%s', orderNumber='%s', productId='%s', quantity=%d, unitPrice=%s, finalAmount=%s}",
                orderItemId, orderNumber, productId, quantity, unitPrice, finalAmount);
    }

}