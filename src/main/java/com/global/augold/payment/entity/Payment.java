// ===============================
// Payment.java - 결제 엔티티 (수정 버전)
// ===============================

package com.global.augold.payment.entity;

import com.global.augold.order.entity.Order;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PAYMENT") // 기존 테이블명과 동일
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @Column(name = "PAYMENT_NUMBER") // 기존 컬럼명과 동일
    private String paymentNumber; // 결제번호 (PK) - PAY-00001 형태, DB 트리거에서 자동 생성

    @Column(name = "ORDER_NUMBER", nullable = false)
    private String orderNumber; // 주문번호 - DB에서 생성된 값 저장

    @Enumerated(EnumType.STRING)
    @Column(name = "PAYMENT_METHOD", nullable = false)
    private PaymentMethod paymentMethod; // 결제수단

    @Column(name = "PAYMENT_AMOUNT", nullable = false, precision = 10, scale = 2)
    private BigDecimal paymentAmount; // 결제금액

    @Enumerated(EnumType.STRING)
    @Column(name = "PAYMENT_STATUS", nullable = false)
    private PaymentStatus paymentStatus; // 결제상태

    @Column(name = "PAYMENT_DATE")
    private LocalDateTime paymentDate; // 결제완료일시

    @Column(name = "CARD_COMPANY")
    private String cardCompany; // 카드사

    @Column(name = "CARD_NUMBER")
    private String cardNumber; // 카드번호 (마스킹 처리)

    @Column(name = "CANCEL_DATE")
    private LocalDateTime cancelDate; // 취소일시

    @Column(name = "CANCEL_REASON")
    private String cancelReason; // 취소사유

    // ===============================
    // 연관관계 설정 (읽기 전용)
    // ===============================

    // Payment(1) : Order(1) 관계 - 읽기 전용
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_NUMBER", insertable = false, updatable = false)
    private Order order; // 주문 정보 조회용 (읽기 전용)

    // 결제 수단
    public enum PaymentMethod {
        CARD("신용카드"),
        BANK_TRANSFER("계좌이체"),
        VIRTUAL_ACCOUNT("가상계좌"),
        KAKAOPAY("카카오페이"),
        NAVERPAY("네이버페이"),
        PAYCO("페이코"),
        TOSS("토스페이"),
        CASH("현금");

        private final String description;

        PaymentMethod(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }


    // 결제 상태 Enum

    public enum PaymentStatus {
        PENDING("결제대기"),      // 결제 요청됨
        PROCESSING("결제처리중"), // 결제 처리 중
        COMPLETED("결제완료"),    // 결제 완료
        FAILED("결제실패"),       // 결제 실패
        CANCELLED("결제취소"),    // 결제 취소
        REFUNDED("환불완료");     // 환불 완료

        private final String description;

        PaymentStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }


    // 비즈니스 메서드

    /**
     * 엔티티 생성 시 기본값 설정
     */
    @PrePersist
    protected void onCreate() {
        // paymentNumber는 DB 트리거에서 자동 생성됨
        if (paymentStatus == null) {
            paymentStatus = PaymentStatus.PENDING;
        }
    }

    /**
     * 생성자 - 새 결제 생성시 사용
     */
    public Payment(String orderNumber, PaymentMethod paymentMethod, BigDecimal paymentAmount) {
        // paymentNumber는 DB 트리거에서 자동 생성됨
        this.orderNumber = orderNumber;
        this.paymentMethod = paymentMethod;
        this.paymentAmount = paymentAmount;
        this.paymentStatus = PaymentStatus.PENDING;
    }

    /**
     * 결제 완료 처리
     */
    public void completePayment() {
        this.paymentStatus = PaymentStatus.COMPLETED;
        this.paymentDate = LocalDateTime.now();
    }

    /**
     * 결제 완료 처리 (상세 정보 포함)
     */
    public void completePayment(String cardCompany, String cardNumber) {
        this.paymentStatus = PaymentStatus.COMPLETED;
        this.paymentDate = LocalDateTime.now();
        setCardInfo(cardNumber, cardCompany);
    }

    /**
     * 결제 실패 처리
     */
    public void failPayment() {
        this.paymentStatus = PaymentStatus.FAILED;
    }

    /**
     * 결제 처리 중 상태로 변경
     */
    public void startProcessing() {
        this.paymentStatus = PaymentStatus.PROCESSING;
    }

    /**
     * 결제 취소 처리
     */
    public void cancelPayment(String reason) {
        this.paymentStatus = PaymentStatus.CANCELLED;
        this.cancelDate = LocalDateTime.now();
        this.cancelReason = reason;
    }

    /**
     * 환불 처리
     */
    public void refundPayment(String reason) {
        this.paymentStatus = PaymentStatus.REFUNDED;
        this.cancelDate = LocalDateTime.now();
        this.cancelReason = reason;
    }

    /**
     * 카드번호 마스킹 처리
     */
    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return cardNumber;
        }

        // 카드번호에서 숫자만 추출
        String cleanNumber = cardNumber.replaceAll("\\D", "");

        if (cleanNumber.length() < 8) {
            return cardNumber;
        }

        // 앞 4자리 + **** + 뒤 4자리 형태로 마스킹
        return cleanNumber.substring(0, 4) +
                "-****-****-" +
                cleanNumber.substring(cleanNumber.length() - 4);
    }

    /**
     * 카드 정보 설정 (마스킹 처리)
     */
    public void setCardInfo(String cardNumber, String cardCompany) {
        this.cardNumber = maskCardNumber(cardNumber);
        this.cardCompany = cardCompany;
    }

    /**
     * 결제 취소 가능 여부 확인
     */
    public boolean isCancellable() {
        return paymentStatus == PaymentStatus.COMPLETED || paymentStatus == PaymentStatus.PENDING;
    }

    /**
     * 환불 가능 여부 확인
     */
    public boolean isRefundable() {
        return paymentStatus == PaymentStatus.COMPLETED;
    }

    /**
     * 결제 상태가 완료인지 확인
     */
    public boolean isCompleted() {
        return paymentStatus == PaymentStatus.COMPLETED;
    }

    /**
     * 결제 상태가 실패인지 확인
     */
    public boolean isFailed() {
        return paymentStatus == PaymentStatus.FAILED;
    }

    /**
     * 결제 상태가 대기 중인지 확인
     */
    public boolean isPending() {
        return paymentStatus == PaymentStatus.PENDING;
    }

    /**
     * 결제 상태가 처리 중인지 확인
     */
    public boolean isProcessing() {
        return paymentStatus == PaymentStatus.PROCESSING;
    }

    /**
     * 결제가 취소되었는지 확인
     */
    public boolean isCancelled() {
        return paymentStatus == PaymentStatus.CANCELLED;
    }

    /**
     * 환불이 완료되었는지 확인
     */
    public boolean isRefunded() {
        return paymentStatus == PaymentStatus.REFUNDED;
    }

    /**
     * 결제 유효성 검증
     */
    public boolean isValid() {
        return orderNumber != null && !orderNumber.trim().isEmpty()
                && paymentMethod != null
                && paymentAmount != null && paymentAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 결제 소요 시간 계산 (완료된 경우에만)
     */
    public long getPaymentDurationMinutes() {
        if (paymentDate == null) {
            return 0;
        }
        // 생성 시간과 완료 시간의 차이 (분 단위)
        // 실제로는 결제 시작 시간을 별도로 관리하는 것이 좋음
        return java.time.Duration.between(paymentDate.minusMinutes(5), paymentDate).toMinutes();
    }

    /**
     * 결제 방법이 카드인지 확인
     */
    public boolean isCardPayment() {
        return paymentMethod == PaymentMethod.CARD;
    }

    /**
     * 결제 방법이 간편결제인지 확인
     */
    public boolean isSimplePayment() {
        return paymentMethod == PaymentMethod.KAKAOPAY
                || paymentMethod == PaymentMethod.NAVERPAY
                || paymentMethod == PaymentMethod.PAYCO
                || paymentMethod == PaymentMethod.TOSS;
    }


    //    결제 정보 요약 문자열

    @Override
    public String toString() {
        return String.format("Payment{paymentNumber='%s', orderNumber='%s', method=%s, amount=%s, status=%s}",
                paymentNumber, orderNumber, paymentMethod, paymentAmount, paymentStatus);
    }

    /**
     * 결제 상세 정보 (디버깅용)
     */
    public String getPaymentDetails() {
        StringBuilder details = new StringBuilder();
        details.append(String.format("결제번호: %s\n", paymentNumber));
        details.append(String.format("주문번호: %s\n", orderNumber));
        details.append(String.format("결제방법: %s\n", paymentMethod.getDescription()));
        details.append(String.format("결제금액: ₩%s\n", paymentAmount));
        details.append(String.format("결제상태: %s\n", paymentStatus.getDescription()));

        if (paymentDate != null) {
            details.append(String.format("결제일시: %s\n", paymentDate));
        }

        if (cardCompany != null && cardNumber != null) {
            details.append(String.format("카드정보: %s %s\n", cardCompany, cardNumber));
        }

        if (cancelDate != null && cancelReason != null) {
            details.append(String.format("취소일시: %s\n", cancelDate));
            details.append(String.format("취소사유: %s\n", cancelReason));
        }

        return details.toString();
    }
}