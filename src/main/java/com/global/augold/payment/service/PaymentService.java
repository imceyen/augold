package com.global.augold.payment.service;

import com.global.augold.cart.service.CartService;
import com.global.augold.member.entity.Customer;
import com.global.augold.member.repository.CustomerRepository;
import com.global.augold.order.entity.Order;
import com.global.augold.order.entity.OrderItem;
import com.global.augold.order.repository.OrderRepository;
import com.global.augold.order.repository.OrderItemRepository;
import com.global.augold.payment.entity.Payment;
import com.global.augold.payment.repository.PaymentRepository;
import com.global.augold.product.entity.Product;
import com.global.augold.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {

    // Repository 주입
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final TossPaymentsService tossPaymentsService;
    private final CartService cartService;

    /**
     * 결제 페이지 데이터 생성
     */
    public PaymentPageData createPaymentPageData(String orderNumber, String cstmNumber) {
        try {
            // 1. 주문 정보 조회
            Order order = orderRepository.findByOrderNumber(orderNumber)
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderNumber));

            // 2. 고객 정보 조회
            Customer customer = customerRepository.findByCstmNumber(cstmNumber)
                    .orElseThrow(() -> new IllegalArgumentException("고객을 찾을 수 없습니다: " + cstmNumber));

            // 3. 주문 상품 목록 조회
            List<OrderItem> orderItemList = orderItemRepository.findByOrderNumber(orderNumber);
            if (orderItemList.isEmpty()) {
                throw new IllegalArgumentException("주문 상품이 없습니다: " + orderNumber);
            }

            // 4. 주문 상품 정보를 PaymentItemData로 변환
            List<PaymentItemData> orderItems = convertToPaymentItemData(orderItemList);

            // 5. 결제 페이지 데이터 생성
            PaymentPageData paymentData = new PaymentPageData();
            paymentData.setOrderNumber(order.getOrderNumber());
            paymentData.setOrderName(generateOrderName(orderItems));
            paymentData.setTotalAmount(order.getFinalAmount());

            // Customer Entity 필드와 매칭 (이메일 제외)
            paymentData.setCustomerNumber(customer.getCstmNumber());
            paymentData.setCustomerName(customer.getCstmName());
            paymentData.setCustomerPhone(customer.getCstmPhone());
            paymentData.setCustomerKey(generateTossCustomerKey(customer));
            paymentData.setOrderItems(orderItems);

            log.info("결제 페이지 데이터 생성 완료: {}", orderNumber);
            return paymentData;

        } catch (Exception e) {
            log.error("결제 페이지 데이터 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("결제 페이지 데이터 생성에 실패했습니다.", e);
        }
    }

    /**
     * 결제 성공 처리 (실제 토스 API + 실제 DB 저장)
     */
    public void processPaymentSuccess(String orderNumber, String paymentKey, BigDecimal amount) {
        try {
            // 1. 실제 토스페이먼츠 API 호출 (테스트 환경)
            TossPaymentsService.TossPaymentResponse tossResponse =
                    tossPaymentsService.confirmPayment(paymentKey, orderNumber, amount);

            // 2. 주문 조회 (실제 DB)
            Order order = orderRepository.findByOrderNumber(orderNumber)
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderNumber));

            // 3. 결제 정보 생성 또는 업데이트 (실제 DB)
            Payment payment = paymentRepository.findByOrderNumber(orderNumber)
                    .orElse(new Payment(orderNumber, getPaymentMethod(tossResponse.getMethod()), amount));

            // Payment ID를 빈 문자열로 설정
            if (payment.getPaymentNumber() == null || payment.getPaymentNumber().isEmpty()) {
                payment.setPaymentNumber("");
            }

            // 4. 결제 완료 처리
            payment.completePayment();

            // 카드 정보 설정 (테스트 데이터)
            if (tossResponse.getCard() != null) {
                payment.setCardInfo(tossResponse.getCard(), "테스트카드");
            }

            // 5. 주문 상태 업데이트 (실제 DB)
            order.completePayment();

            // 6. 🔥 재고 차감 처리 (실제 DB)
            List<OrderItem> orderItems = orderItemRepository.findByOrderNumber(orderNumber);
            for (OrderItem item : orderItems) {
                Product product = productRepository.findByProductId(item.getProductId())
                        .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + item.getProductId()));

                // 재고 차감
                if (product.getProductInventory() >= item.getQuantity()) {
                    product.setProductInventory(product.getProductInventory() - item.getQuantity());
                    productRepository.save(product);
                    log.info("재고 차감 완료: 상품={}, 차감수량={}, 남은재고={}",
                            product.getProductName(), item.getQuantity(), product.getProductInventory());
                } else {
                    // 재고 부족 시 결제 취소
                    log.error("재고 부족으로 결제 취소: 상품={}, 주문수량={}, 현재재고={}",
                            product.getProductName(), item.getQuantity(), product.getProductInventory());

                    tossPaymentsService.cancelPayment(paymentKey, "재고 부족", amount);

                    throw new IllegalArgumentException("재고가 부족합니다: " + product.getProductName() +
                            " (주문: " + item.getQuantity() + "개, 재고: " + product.getProductInventory() + "개)");
                }
            }

            // 7. 저장 (실제 DB)
            paymentRepository.save(payment);
            orderRepository.save(order);

            // 8. 결제 성공 시에만 장바구니 비우기
            cartService.deleteAllItems(order.getCstmNumber());


        } catch (Exception e) {
            throw new RuntimeException("결제 처리에 실패했습니다.", e);
        }
    }

    /**
     * 토스페이먼츠 결제수단을 Payment.PaymentMethod로 변환
     */
    private Payment.PaymentMethod getPaymentMethod(String tossMethod) {
        if (tossMethod == null) return Payment.PaymentMethod.CARD;

        switch (tossMethod.toLowerCase()) {
            case "카드":
            case "card":
                return Payment.PaymentMethod.CARD;
            case "계좌이체":
                return Payment.PaymentMethod.BANK_TRANSFER;
            case "가상계좌":
                return Payment.PaymentMethod.VIRTUAL_ACCOUNT;
            default:
                return Payment.PaymentMethod.CARD;
        }
    }

    /**
     * 결제 실패 처리 (실제 DB 처리)
     */
    public void processPaymentFailure(String orderNumber, String errorCode, String errorMessage) {
        try {
            // 결제 실패 로그 기록
            log.warn("결제 실패: 주문번호={}, 오류코드={}, 오류메시지={}", orderNumber, errorCode, errorMessage);

            // 주문 상태를 실패로 변경 (실제 DB)
            Order order = orderRepository.findByOrderNumber(orderNumber).orElse(null);
            if (order != null) {
                order.updateStatus(Order.OrderStatus.CANCELLED);
                orderRepository.save(order);
                log.info("주문 상태를 취소로 변경: {}", orderNumber);
            }

        } catch (Exception e) {
            log.error("결제 실패 처리 중 오류: {}", e.getMessage(), e);
        }
    }

    /**
     * OrderItem을 PaymentItemData로 변환
     */
    private List<PaymentItemData> convertToPaymentItemData(List<OrderItem> orderItems) {
        List<PaymentItemData> paymentItems = new ArrayList<>();

        for (OrderItem orderItem : orderItems) {
            try {
                // 상품 정보 조회
                Product product = productRepository.findByProductId(orderItem.getProductId())
                        .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + orderItem.getProductId()));

                PaymentItemData paymentItem = new PaymentItemData();
                paymentItem.setProductId(orderItem.getProductId());
                paymentItem.setProductName(product.getProductName());
                paymentItem.setKaratCode(product.getKaratCode());
                paymentItem.setQuantity(orderItem.getQuantity());
                paymentItem.setUnitPrice(orderItem.getUnitPrice());
                paymentItem.setFinalAmount(orderItem.getFinalAmount());
                paymentItem.setImageUrl(product.getImageUrl() != null ? product.getImageUrl() : "/images/default-product.jpg");

                paymentItems.add(paymentItem);

            } catch (Exception e) {
                log.error("상품 정보 변환 실패: productId={}, error={}", orderItem.getProductId(), e.getMessage());
                // 상품 정보를 찾을 수 없는 경우 기본값으로 처리
                PaymentItemData paymentItem = new PaymentItemData();
                paymentItem.setProductId(orderItem.getProductId());
                paymentItem.setProductName("상품명 없음");
                paymentItem.setKaratCode("N/A");
                paymentItem.setQuantity(orderItem.getQuantity());
                paymentItem.setUnitPrice(orderItem.getUnitPrice());
                paymentItem.setFinalAmount(orderItem.getFinalAmount());
                paymentItem.setImageUrl("/images/default-product.jpg");

                paymentItems.add(paymentItem);
            }
        }

        return paymentItems;
    }

    /**
     * 토스페이먼츠용 고객키 생성 (이메일 없이)
     */
    private String generateTossCustomerKey(Customer customer) {
        String rawKey = "customer_" + customer.getCstmNumber() + "_" + customer.getCstmPhone().replace("-", "");
        return java.util.Base64.getEncoder().encodeToString(rawKey.getBytes());
    }

    /**
     * 주문명 생성 (토스페이먼츠용)
     */
    private String generateOrderName(List<PaymentItemData> orderItems) {
        if (orderItems.isEmpty()) {
            return "골드 상품";
        }

        String firstProductName = orderItems.get(0).getProductName();
        int totalItemCount = orderItems.size();

        if (totalItemCount == 1) {
            return firstProductName;
        } else {
            return firstProductName + " 외 " + (totalItemCount - 1) + "건";
        }
    }

    // ===============================
    // DTO 클래스들
    // ===============================

    /**
     * 결제 페이지 데이터 DTO
     */
    public static class PaymentPageData {
        private String orderNumber;
        private String orderName;
        private BigDecimal totalAmount;
        private String customerNumber;
        private String customerName;
        private String customerPhone;
        private String customerKey;
        private List<PaymentItemData> orderItems;

        // Getters and Setters
        public String getOrderNumber() { return orderNumber; }
        public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

        public String getOrderName() { return orderName; }
        public void setOrderName(String orderName) { this.orderName = orderName; }

        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

        public String getCustomerNumber() { return customerNumber; }
        public void setCustomerNumber(String customerNumber) { this.customerNumber = customerNumber; }

        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }

        public String getCustomerPhone() { return customerPhone; }
        public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

        public String getCustomerKey() { return customerKey; }
        public void setCustomerKey(String customerKey) { this.customerKey = customerKey; }

        public List<PaymentItemData> getOrderItems() { return orderItems; }
        public void setOrderItems(List<PaymentItemData> orderItems) { this.orderItems = orderItems; }
    }

    /**
     * 결제 상품 정보 DTO
     */
    public static class PaymentItemData {
        private String productId;
        private String productName;
        private String karatCode;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal finalAmount;
        private String imageUrl;

        // Getters and Setters
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public String getKaratCode() { return karatCode; }
        public void setKaratCode(String karatCode) { this.karatCode = karatCode; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

        public BigDecimal getFinalAmount() { return finalAmount; }
        public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    }
}