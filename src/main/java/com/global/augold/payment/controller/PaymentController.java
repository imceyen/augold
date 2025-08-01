package com.global.augold.payment.controller;

import com.global.augold.order.service.OrderService;
import com.global.augold.payment.service.PaymentService;
import com.global.augold.payment.service.PaymentService.PaymentPageData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.global.augold.member.entity.Customer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;

    /**
     * 결제 요청 페이지
     * URL: /payment/request?orderNumber=ORD-001
     */
    @GetMapping("/request")
    public String paymentRequest(@RequestParam String orderNumber,
                                 HttpSession session,
                                 Model model) {
        try {
            Customer loginUser = (Customer) session.getAttribute("loginUser");
            if (loginUser == null) {
                log.warn("로그인되지 않은 사용자의 결제 요청: {}", orderNumber);
                return "redirect:/login?returnUrl=/payment/request?orderNumber=" + orderNumber;
            }

            String cstmNumber = loginUser.getCstmNumber();

            model.addAttribute("loginName", loginUser.getCstmName());

            // 결제 페이지 데이터 생성
            PaymentPageData paymentData = paymentService.createPaymentPageData(orderNumber, cstmNumber);

            // HTML 템플릿에서 사용할 변수들 설정
            model.addAttribute("orderNumber", paymentData.getOrderNumber());
            model.addAttribute("orderName", paymentData.getOrderName());
            model.addAttribute("totalAmount", paymentData.getTotalAmount());
            model.addAttribute("customerNumber", paymentData.getCustomerNumber());
            model.addAttribute("customerName", paymentData.getCustomerName());
            model.addAttribute("customerPhone", paymentData.getCustomerPhone());
            model.addAttribute("customerKey", paymentData.getCustomerKey());
            model.addAttribute("orderItems", paymentData.getOrderItems());

            // 🔥 바로구매 여부 확인하여 모델에 추가
            boolean isDirectBuy = paymentService.isDirectBuyOrder(orderNumber);
            model.addAttribute("isDirectBuy", isDirectBuy);

            log.info("결제 페이지 요청 처리 완료: 주문번호={}, 고객번호={}", orderNumber, cstmNumber);
            return "payment/payment-request";

        } catch (Exception e) {
            log.error("결제 페이지 요청 처리 실패: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "결제 페이지를 불러오는 중 오류가 발생했습니다.");
            return "error/payment-error";
        }
    }

    /**
     * 토스페이먼츠 결제 성공 페이지
     * URL: /payment/toss/success?paymentKey=...&orderId=...&amount=...
     */
    @GetMapping("/toss/success")
    public String tossPaymentSuccess(@RequestParam String paymentKey,
                                     @RequestParam String orderId,
                                     @RequestParam String amount,
                                     HttpSession session,
                                     Model model) {
        try {
            Customer loginUser = (Customer) session.getAttribute("loginUser");
            if (loginUser != null) {
                model.addAttribute("loginName", loginUser.getCstmName());
            }
            log.info("토스페이먼츠 결제 성공 콜백: paymentKey={}, orderId={}, amount={}",
                    paymentKey, orderId, amount);

            // URL 파라미터를 템플릿에 전달 (JavaScript에서 사용)
            model.addAttribute("paymentKey", paymentKey);
            model.addAttribute("orderId", orderId);
            model.addAttribute("amount", amount);

            return "payment/payment-success"; // payment-success.html

        } catch (Exception e) {
            log.error("결제 성공 페이지 처리 실패: {}", e.getMessage(), e);
            return "redirect:/payment/toss/fail?code=SYSTEM_ERROR&message=" + e.getMessage();
        }
    }

    /**
     * 토스페이먼츠 결제 실패 페이지
     * URL: /payment/toss/fail?code=...&message=...
     */
    @GetMapping("/toss/fail")
    public String tossPaymentFail(@RequestParam String code,
                                  @RequestParam String message,
                                  HttpSession session,
                                  Model model) {
        try {
            Customer loginUser = (Customer) session.getAttribute("loginUser");
            if (loginUser != null) {
                model.addAttribute("loginName", loginUser.getCstmName());
            }
            log.warn("토스페이먼츠 결제 실패: code={}, message={}", code, message);

            // URL 파라미터를 템플릿에 전달 (JavaScript에서 사용)
            model.addAttribute("errorCode", code);
            model.addAttribute("errorMessage", message);

            return "payment/payment-fail"; // payment-fail.html

        } catch (Exception e) {
            log.error("결제 실패 페이지 처리 실패: {}", e.getMessage(), e);
            model.addAttribute("errorCode", "SYSTEM_ERROR");
            model.addAttribute("errorMessage", "시스템 오류가 발생했습니다.");
            return "payment/payment-fail";
        }
    }

    // ===============================
    // REST API 엔드포인트들
    // ===============================

    /**
     * 토스페이먼츠 결제 승인 API (실제 API 호출 - 테스트 키 사용)
     * URL: POST /payment/api/toss/confirm
     */
    @PostMapping("/api/toss/confirm")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> confirmTossPayment(@RequestBody Map<String, Object> requestData) {
        Map<String, Object> response = new HashMap<>();

        try {
            String paymentKey = (String) requestData.get("paymentKey");
            String orderId = (String) requestData.get("orderId");
            Integer amount = (Integer) requestData.get("amount");

            log.info("실제 토스페이먼츠 결제 승인 요청 (테스트): paymentKey={}, orderId={}, amount={}",
                    paymentKey, orderId, amount);

            // 🔥 실제 토스페이먼츠 API를 통한 결제 승인 + 실제 DB 저장 + 재고 차감
            paymentService.processPaymentSuccess(orderId, paymentKey, BigDecimal.valueOf(amount));

            // 성공 응답
            response.put("success", true);
            response.put("message", "결제가 성공적으로 완료되었습니다. (테스트 환경)");
            response.put("paymentKey", paymentKey);
            response.put("orderId", orderId);
            response.put("amount", amount);
            response.put("testMode", true); // 테스트 모드임을 표시

            log.info("실제 토스페이먼츠 결제 승인 완료 (테스트): orderId={}", orderId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // 재고 부족 등의 비즈니스 로직 오류
            log.warn("결제 승인 실패 (비즈니스 로직): {}", e.getMessage());

            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("errorType", "BUSINESS_ERROR");

            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            // 시스템 오류
            log.error("실제 토스페이먼츠 결제 승인 실패: {}", e.getMessage(), e);

            response.put("success", false);
            response.put("message", "결제 승인에 실패했습니다: " + e.getMessage());
            response.put("errorType", "SYSTEM_ERROR");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 결제 실패 로그 API
     * URL: POST /payment/api/log-failure
     */
    @PostMapping("/api/log-failure")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> logPaymentFailure(@RequestBody Map<String, Object> requestData) {
        Map<String, Object> response = new HashMap<>();

        try {
            String errorCode = (String) requestData.get("errorCode");
            String errorMessage = (String) requestData.get("errorMessage");
            String timestamp = (String) requestData.get("timestamp");
            String userAgent = (String) requestData.get("userAgent");

            log.warn("결제 실패 로그: code={}, message={}, timestamp={}, userAgent={}",
                    errorCode, errorMessage, timestamp, userAgent);

            // 결제 실패 로그를 DB에 저장하거나 추가 처리
            // paymentService.logPaymentFailure(errorCode, errorMessage, timestamp, userAgent);

            response.put("success", true);
            response.put("message", "로그가 기록되었습니다.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("결제 실패 로그 기록 실패: {}", e.getMessage(), e);

            response.put("success", false);
            response.put("message", "로그 기록에 실패했습니다.");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 주문 정보 조회 API (AJAX용)
     * URL: GET /payment/api/order/{orderNumber}
     */
    @GetMapping("/api/order/{orderNumber}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOrderInfo(@PathVariable String orderNumber,
                                                            HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        try {
            Customer loginUser = (Customer) session.getAttribute("loginUser");
            if (loginUser == null) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String cstmNumber = loginUser.getCstmNumber();
            if (cstmNumber == null) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            PaymentPageData paymentData = paymentService.createPaymentPageData(orderNumber, cstmNumber);

            response.put("success", true);
            response.put("data", paymentData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("주문 정보 조회 실패: {}", e.getMessage(), e);

            response.put("success", false);
            response.put("message", "주문 정보 조회에 실패했습니다.");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/api/handle-failed-direct-buy")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleFailedDirectBuy(@RequestBody Map<String, Object> requestData,
                                                                     HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        try {
            Customer loginUser = (Customer) session.getAttribute("loginUser");
            if (loginUser == null) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String orderNumber = (String) requestData.get("orderNumber");
            String action = (String) requestData.get("action"); // "addToCart" or "goHome"

            if ("addToCart".equals(action)) {
                // 🔥 주문 상품들을 장바구니에 추가하고 주문 취소
                boolean result = paymentService.moveFailedOrderToCart(orderNumber, loginUser.getCstmNumber());

                if (result) {
                    response.put("success", true);
                    response.put("message", "상품이 장바구니에 추가되었습니다.");
                    response.put("redirectUrl", "/cart");
                } else {
                    response.put("success", false);
                    response.put("message", "장바구니 추가에 실패했습니다.");
                }
            } else {
                // 🔥 그냥 주문만 취소하고 홈으로
                paymentService.cancelFailedOrder(orderNumber, loginUser.getCstmNumber());
                response.put("success", true);
                response.put("message", "주문이 취소되었습니다.");
                response.put("redirectUrl", "/");
            }

            log.info("바로구매 결제 실패 처리: 주문번호={}, 액션={}, 고객={}",
                    orderNumber, action, loginUser.getCstmNumber());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("바로구매 결제 실패 처리 실패: {}", e.getMessage(), e);

            response.put("success", false);
            response.put("message", "처리 중 오류가 발생했습니다.");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 주문이 바로구매인지 확인하는 API
     * URL: GET /payment/api/is-direct-buy/{orderNumber}
     */
    @GetMapping("/api/is-direct-buy/{orderNumber}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> isDirectBuyOrder(@PathVariable String orderNumber,
                                                                HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        try {
            Customer loginUser = (Customer) session.getAttribute("loginUser");
            if (loginUser == null) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // 🔥 주문의 상품 개수로 바로구매 여부 판단 (간단한 방법)
            boolean isDirectBuy = paymentService.isDirectBuyOrder(orderNumber);

            response.put("success", true);
            response.put("isDirectBuy", isDirectBuy);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("바로구매 주문 확인 실패: {}", e.getMessage(), e);

            response.put("success", false);
            response.put("message", "확인 중 오류가 발생했습니다.");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // PaymentController에서
    @GetMapping("/cancel-and-add-to-cart")
    public String cancelOrderAndAddToCart(@RequestParam String orderNumber,
                                          HttpSession session) {
        try {
            Customer loginUser = (Customer) session.getAttribute("loginUser");
            if (loginUser == null) {
                return "redirect:/login";
            }

            // 🔥 기존 OrderController API 활용
            boolean result = orderService.moveOrderToCart(orderNumber, loginUser.getCstmNumber());

            if (result) {
                return "redirect:/cart?added=true&from=payment";
            } else {
                return "redirect:/cart?error=add_failed";
            }

        } catch (Exception e) {
            log.error("결제 취소 후 장바구니 추가 실패: {}", e.getMessage(), e);
            return "redirect:/cart?error=system";
        }
    }
}