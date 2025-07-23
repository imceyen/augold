package com.global.augold.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TossPaymentsService {

    // ✅ 테스트 시크릿 키 (실제 돈 안나감)
    private static final String SECRET_KEY = "test_gsk_docs_OaPz8L5KdmQXkzRz3y47BMw6";
    private static final String TOSS_API_URL = "https://api.tosspayments.com/v1/payments/confirm";

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 토스페이먼츠 결제 승인 (실제 API 호출 - 테스트 환경)
     */
    public TossPaymentResponse confirmPayment(String paymentKey, String orderId, BigDecimal amount) {
        try {
            log.info("🔄 실제 토스페이먼츠 API 호출 시작 (테스트): paymentKey={}, orderId={}, amount={}",
                    paymentKey, orderId, amount);

            // 1. 토스 API 인증 헤더 생성
            String encryptedSecretKey = "Basic " +
                    Base64.getEncoder().encodeToString((SECRET_KEY + ":").getBytes());

            // 2. 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", encryptedSecretKey);
            headers.set("Content-Type", "application/json");

            // 3. 요청 바디 생성
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("orderId", orderId);
            requestBody.put("amount", amount.intValue());
            requestBody.put("paymentKey", paymentKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 4. 토스 API 호출
            ResponseEntity<Map> response = restTemplate.postForEntity(TOSS_API_URL, request, Map.class);
            Map<String, Object> responseBody = response.getBody();

            log.info("✅ 토스페이먼츠 API 응답 성공: status={}, method={}",
                    responseBody.get("status"), responseBody.get("method"));

            // 5. 응답을 TossPaymentResponse로 변환
            TossPaymentResponse tossResponse = new TossPaymentResponse();
            tossResponse.setPaymentKey(paymentKey);
            tossResponse.setOrderId(orderId);
            tossResponse.setOrderName((String) responseBody.get("orderName"));
            tossResponse.setStatus((String) responseBody.get("status"));
            tossResponse.setTotalAmount(BigDecimal.valueOf(((Number) responseBody.get("totalAmount")).longValue()));
            tossResponse.setMethod((String) responseBody.get("method"));
            tossResponse.setRequestedAt((String) responseBody.get("requestedAt"));
            tossResponse.setApprovedAt((String) responseBody.get("approvedAt"));

            // 카드 정보 처리
            Map<String, Object> card = (Map<String, Object>) responseBody.get("card");
            if (card != null) {
                String cardCompany = (String) card.get("company");
                String cardNumber = (String) card.get("number");
                tossResponse.setCard(cardCompany + " " + cardNumber);
            }

            log.info("🎉 실제 토스페이먼츠 결제 승인 완료 (테스트): orderId={}, amount={}", orderId, amount);
            return tossResponse;

        } catch (HttpClientErrorException e) {
            log.error("❌ 토스페이먼츠 API 오류: status={}, response={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("토스 결제 승인 실패: " + e.getResponseBodyAsString());

        } catch (Exception e) {
            log.error("❌ 토스페이먼츠 API 호출 실패: {}", e.getMessage(), e);
            throw new RuntimeException("토스 결제 승인에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 토스페이먼츠 결제 취소 (실제 API 호출)
     */
    public TossPaymentResponse cancelPayment(String paymentKey, String cancelReason, BigDecimal cancelAmount) {
        try {
            log.info("🔄 실제 토스페이먼츠 취소 API 호출 시작: paymentKey={}, reason={}, amount={}",
                    paymentKey, cancelReason, cancelAmount);

            // 취소 API URL
            String cancelUrl = "https://api.tosspayments.com/v1/payments/" + paymentKey + "/cancel";

            // 인증 헤더 생성
            String encryptedSecretKey = "Basic " +
                    Base64.getEncoder().encodeToString((SECRET_KEY + ":").getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", encryptedSecretKey);
            headers.set("Content-Type", "application/json");

            // 취소 요청 바디
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("cancelReason", cancelReason);
            if (cancelAmount != null) {
                requestBody.put("cancelAmount", cancelAmount.intValue());
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // API 호출
            ResponseEntity<Map> response = restTemplate.postForEntity(cancelUrl, request, Map.class);
            Map<String, Object> responseBody = response.getBody();

            // 응답 변환
            TossPaymentResponse tossResponse = new TossPaymentResponse();
            tossResponse.setPaymentKey(paymentKey);
            tossResponse.setStatus((String) responseBody.get("status"));
            tossResponse.setTotalAmount(BigDecimal.valueOf(((Number) responseBody.get("totalAmount")).longValue()));

            log.info("✅ 토스페이먼츠 취소 성공: paymentKey={}", paymentKey);
            return tossResponse;

        } catch (Exception e) {
            log.error("❌ 토스페이먼츠 취소 실패: {}", e.getMessage(), e);
            throw new RuntimeException("결제 취소 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 토스페이먼츠 API 응답 DTO
     */
    public static class TossPaymentResponse {
        private String paymentKey;
        private String orderId;
        private String orderName;
        private String status;
        private BigDecimal totalAmount;
        private String method;
        private String requestedAt;
        private String approvedAt;
        private String card;

        // Getters and Setters
        public String getPaymentKey() { return paymentKey; }
        public void setPaymentKey(String paymentKey) { this.paymentKey = paymentKey; }

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }

        public String getOrderName() { return orderName; }
        public void setOrderName(String orderName) { this.orderName = orderName; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }

        public String getRequestedAt() { return requestedAt; }
        public void setRequestedAt(String requestedAt) { this.requestedAt = requestedAt; }

        public String getApprovedAt() { return approvedAt; }
        public void setApprovedAt(String approvedAt) { this.approvedAt = approvedAt; }

        public String getCard() { return card; }
        public void setCard(String card) { this.card = card; }

        @Override
        public String toString() {
            return String.format("TossPaymentResponse{paymentKey='%s', orderId='%s', status='%s', totalAmount=%s}",
                    paymentKey, orderId, status, totalAmount);
        }
    }
}