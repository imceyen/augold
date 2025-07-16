// ===============================
// Order.java - 주문 엔티티
// ===============================

package com.global.augold.order.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ORDERS") // 기존 테이블명과 동일
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @Column(name = "ORDER_NUMBER")
    private String orderNumber; // 자동 생성

    @Column(name = "CSTM_NUMBER", nullable = false)
    private String cstmNumber; // 회원 번호

    @Column(name = "ORDER_DATE", nullable = false)
    private LocalDateTime orderDate; // 주문일시

    @Enumerated(EnumType.STRING) // 이 필드는 enum 타입이라고 선언, DB에 저장할때 Enum 문자열로 변환해서 저장해라. 쓰는 이유 : 타입 안전성, IDE 자동완성, 컴파일 타임 체크, 코드 가독성, 리펙토링 안전성
    @Column(name = "ORDER_STATUS", nullable = false) // Order_Status 랑 매칭된다
    private OrderStatus orderStatus; // 주문상태

    @Column(name = "TOTAL_AMOUNT", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount; // 총 주문금액

    @Column(name = "FINAL_AMOUNT", nullable = false, precision = 10, scale = 2)
    private BigDecimal finalAmount; // 최종 결제 금액

    @Column(name = "DELIVERY_ADDR", nullable = false, columnDefinition = "TEXT")
    private String deliveryAddr; // 배송주소

    @Column(name = "DELIVERY_PHONE", nullable = false)
    private String deliveryPhone; // 배송연락처



    // Order  : OrderItem 일 대 다 관계
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems;

    // Order : Payment 일 대 일 관계
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Payment payment;


    // 주문 상태
    public enum OrderStatus {
        PENDING("대기"),           // 주문 생성
        PAID("결제완료"),          // 결제 완료
        PREPARING("상품준비중"),    // 상품 준비 중
        SHIPPED("배송중"),         // 배송 중
        DELIVERED("배송완료"),     // 배송 완료
        CANCELLED("취소"),         // 주문 취소
        REFUNDED("환불완료");      // 환불 완료

        private final String description;

        OrderStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }


     // 주문 생성 시 기본값 설정

    @PrePersist
    protected void onCreate() {
        if (orderDate == null) {
            orderDate = LocalDateTime.now();
        }
        if (orderStatus == null) {
            orderStatus = OrderStatus.PENDING;
        }
        // orderNumber는 DB 트리거에서 자동 생성됨
    }


    // 주문 상태 변경 메서드

    public void updateStatus(OrderStatus newStatus) {
        this.orderStatus = newStatus;
    }


    //  결제 완료 처리

    public void completePayment() {
        this.orderStatus = OrderStatus.PAID;
    }


    // 주문 취소 처리

    public void cancelOrder() {
        this.orderStatus = OrderStatus.CANCELLED;
    }


    // 총 주문 상품 개수 계산

    public int getTotalItemCount() {
        if (orderItems == null || orderItems.isEmpty()) {
            return 0;
        }
        return orderItems.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }


    //  주문 상태가 취소 가능한지 확인

    public boolean isCancellable() {
        return orderStatus == OrderStatus.PENDING || orderStatus == OrderStatus.PAID;
    }


     // 주문 상태가 환불 가능한지 확인

    public boolean isRefundable() {
        return orderStatus == OrderStatus.DELIVERED;
    }
}