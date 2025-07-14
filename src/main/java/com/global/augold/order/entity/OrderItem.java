// ===============================
// OrderItem.java - 주문 상품 엔티티
// ===============================

package com.global.augold.order.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "ORDER_ITEM") // 기존 테이블명과 동일
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @Column(name = "ORDER_ITEM_ID") // 기존 컬럼명과 동일
    private String orderItemId; // 주문상세ID (PK) - OIT-00001 형태

    @Column(name = "ORDER_NUMBER", nullable = false)
    private String orderNumber; // 주문번호

    @Column(name = "PRODUCT_ID", nullable = false)
    private String productId; // 상품번호

    @Column(name = "QUANTITY", nullable = false)
    private Integer quantity; // 수량

    @Column(name = "UNIT_PRICE", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice; // 단가

    @Column(name = "FINAL_AMOUNT", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalAmount; // 최종금액 (단가 × 수량)

    // ===============================
    // 연관관계 설정
    // ===============================

    // OrderItem (N) : Order (1) 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_NUMBER", insertable = false, updatable = false)
    private Order order;

    // ===============================
    // 비즈니스 메서드
    // ===============================

    /**
     * 엔티티 생성 시 기본값 설정
     */
    @PrePersist
    protected void onCreate() {
        // orderItemId는 DB 트리거에서 자동 생성됨
        if (quantity == null) {
            quantity = 1;
        }
        // 최종금액 자동 계산
        calculateFinalAmount();
    }

    /**
     * 엔티티 수정 시 최종금액 재계산
     */
    @PreUpdate
    protected void onUpdate() {
        calculateFinalAmount();
    }

    /**
     * 최종금액 계산 (단가 × 수량)
     */
    public void calculateFinalAmount() {
        if (unitPrice != null && quantity != null) {
            this.finalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    /**
     * 생성자 - Cart에서 OrderItem으로 변환할 때 사용
     */
    public OrderItem(String orderNumber, String productId, Integer quantity, BigDecimal unitPrice) {
        this.orderNumber = orderNumber;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        // orderItemId는 DB 트리거에서 자동 생성됨
        calculateFinalAmount();
    }

    /**
     * 수량 변경 메서드
     */
    public void updateQuantity(Integer newQuantity) {
        if (newQuantity != null && newQuantity > 0) {
            this.quantity = newQuantity;
            calculateFinalAmount();
        }
    }

    /**
     * 단가 변경 메서드
     */
    public void updateUnitPrice(BigDecimal newUnitPrice) {
        if (newUnitPrice != null && newUnitPrice.compareTo(BigDecimal.ZERO) > 0) {
            this.unitPrice = newUnitPrice;
            calculateFinalAmount();
        }
    }

    /**
     * 상품 총 가격 반환 (finalAmount와 동일하지만 명시적)
     */
    public BigDecimal getTotalPrice() {
        return finalAmount != null ? finalAmount : BigDecimal.ZERO;
    }

    /**
     * CartDTO에서 OrderItem으로 변환하는 정적 메서드
     */
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

    /**
     * 주문상품 정보 요약 문자열
     */
    @Override
    public String toString() {
        return String.format("OrderItem{orderItemId='%s', productId='%s', quantity=%d, unitPrice=%s, finalAmount=%s}",
                orderItemId, productId, quantity, unitPrice, finalAmount);
    }
}