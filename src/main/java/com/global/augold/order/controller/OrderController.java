package com.global.augold.order.controller;

import com.global.augold.cart.dto.CartDTO;
import com.global.augold.cart.service.CartService;
import com.global.augold.member.entity.Customer;
import com.global.augold.order.entity.Order;
import com.global.augold.order.entity.OrderItem;
import com.global.augold.order.service.OrderService;
import com.global.augold.order.dto.OrderCreateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;

    /**
     * 주문 페이지 (장바구니에서 넘어온 경우)
     * URL: GET /order
     */
    @GetMapping("")
    public String orderPage(HttpSession session,
                            Model model,
                            @RequestParam(required = false) String selectedProducts) {
        try {
            // 세션에서 고객 정보 가져오기
            Customer loginUser = (Customer) session.getAttribute("loginUser");
            if (loginUser == null) {
                log.warn("로그인되지 않은 사용자의 주문 요청");
                return "redirect:/login?returnUrl=/cart";
            }

            String cstmNumber = loginUser.getCstmNumber();
            model.addAttribute("loginName", loginUser.getCstmName());

            // 🔥 전체 장바구니에서 상품 목록 가져오기
            List<CartDTO> allCartItems = cartService.getCartList(cstmNumber);
            if (allCartItems.isEmpty()) {
                log.warn("장바구니가 비어있는 상태에서 주문 요청: {}", cstmNumber);
                return "redirect:/cart?error=empty";
            }

            // 🔥 선택된 상품만 필터링
            List<CartDTO> cartItems;
            if (selectedProducts != null && !selectedProducts.isEmpty()) {
                // 선택된 상품 ID:K값 목록
                List<String> selectedProductKeys = Arrays.asList(selectedProducts.split(","));

                // 선택된 상품만 필터링 (productId + karatCode 조합으로)
                cartItems = allCartItems.stream()
                        .filter(item -> {
                            String itemKey = item.getProductId() + ":" + item.getKaratName();
                            return selectedProductKeys.contains(itemKey);
                        })
                        .collect(Collectors.toList());

                log.info("선택된 상품으로 주문: 전체={}, 선택={}", allCartItems.size(), cartItems.size());
            } else {
                // 파라미터가 없으면 전체 장바구니
                cartItems = allCartItems;
            }

            // 🔥 필터링 후 빈 상품 체크
            if (cartItems.isEmpty()) {
                log.warn("선택된 상품이 없음: {}", cstmNumber);
                return "redirect:/cart?error=empty";
            }

            // 🔥 선택된 상품들로 총 금액 계산
            double totalAmount = cartItems.stream()
                    .mapToDouble(item -> item.getFinalPrice() * item.getQuantity())
                    .sum();

            // 🔥 선택된 상품들로 총 수량 계산
            int totalQuantity = cartItems.stream()
                    .mapToInt(CartDTO::getQuantity)
                    .sum();

            // 주문 페이지에 필요한 데이터 설정
            model.addAttribute("cartItems", cartItems);
            model.addAttribute("totalAmount", totalAmount);
            model.addAttribute("totalQuantity", totalQuantity);
            model.addAttribute("customerName", loginUser.getCstmName());
            model.addAttribute("customerPhone", loginUser.getCstmPhone());
            model.addAttribute("customerAddr", loginUser.getCstmAddr());

            log.info("주문 페이지 요청 처리: 고객={}, 상품수={}, 총금액={}",
                    cstmNumber, cartItems.size(), totalAmount);

            return "order/order";

        } catch (Exception e) {
            log.error("주문 페이지 요청 처리 실패: {}", e.getMessage(), e);
            return "redirect:/cart?error=system";
        }
    }

    /**
     * 주문 생성 및 결제 페이지로 이동
     * URL: POST /order/create
     */
    @PostMapping("/create")
    public String createOrder(@ModelAttribute OrderCreateRequest orderRequest,
                              HttpSession session,
                              Model model,
                              @RequestParam(required = false) String selectedProducts,
                              @RequestParam(required = false) String directBuy) {  // 🔥 추가
        try {
            Customer loginUser = (Customer) session.getAttribute("loginUser");

            if (loginUser == null) {
                return "redirect:/login?returnUrl=/cart";
            }

            String cstmNumber = loginUser.getCstmNumber();
            orderRequest.setCstmNumber(cstmNumber);

            if (selectedProducts != null && !selectedProducts.isEmpty()) {
                orderRequest.setSelectedProductIds(selectedProducts);
            }

            // 🔥 바로구매인 경우 기본 배송정보 설정
            if ("true".equals(directBuy)) {
                // 고객 정보에서 기본 배송정보 가져오기
                orderRequest.setDeliveryAddr(loginUser.getCstmAddr() != null ? loginUser.getCstmAddr() : "배송지 정보 없음");
                orderRequest.setDeliveryPhone(loginUser.getCstmPhone() != null ? loginUser.getCstmPhone() : "연락처 정보 없음");
            }

            String orderNumber = orderService.createOrderFromCart(orderRequest, cstmNumber);
            return "redirect:/payment/request?orderNumber=" + orderNumber;  // 👈 원래처럼 간결하게

        } catch (OrderService.StockShortageException e) {
            log.error("❌ StockShortageException: {}", e.getMessage());
            return "redirect:/order?stockShortage=true&productName=" +
                    java.net.URLEncoder.encode(e.getProductName(), java.nio.charset.StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("❌ 예상치 못한 예외: {}", e.getMessage(), e);
            return "redirect:/order?error=system";
        }
    }

    /**
     * 주문 상세 조회 API (JSON 반환) - 수정된 버전
     */
    @GetMapping("/api/{orderNumber}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOrderDetailApi(@PathVariable String orderNumber,
                                                                 HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        try {
            Customer loginUser = (Customer) session.getAttribute("loginUser");
            if (loginUser == null) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(response);
            }

            // 주문 상세 정보 조회
            Order order = orderService.getOrderDetail(orderNumber, loginUser.getCstmNumber());

            // 안전한 JSON 구조로 변환
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("orderNumber", order.getOrderNumber());
            orderData.put("orderDate", order.getOrderDate().toString());
            orderData.put("orderStatus", Map.of(
                    "name", order.getOrderStatus().name(),
                    "description", order.getOrderStatus().getDescription()
            ));
            orderData.put("totalAmount", order.getTotalAmount());
            orderData.put("finalAmount", order.getFinalAmount());
            orderData.put("deliveryAddr", order.getDeliveryAddr());
            orderData.put("deliveryPhone", order.getDeliveryPhone());

            // OrderItem 정보를 안전한 형태로 변환
            List<Map<String, Object>> orderItemsData = new ArrayList<>();
            if (order.getOrderItems() != null) {
                for (OrderItem item : order.getOrderItems()) {
                    Map<String, Object> itemData = new HashMap<>();
                    itemData.put("productId", item.getProductId());
                    itemData.put("productName", item.getProductName() != null ? item.getProductName() : "상품명 없음");
                    itemData.put("imageUrl", item.getImageUrl());
                    itemData.put("quantity", item.getQuantity());
                    itemData.put("unitPrice", item.getUnitPrice());
                    itemData.put("finalAmount", item.getFinalAmount());

                    orderItemsData.add(itemData);
                }
            }
            orderData.put("orderItems", orderItemsData);

            response.put("success", true);
            response.put("order", orderData);

            log.info("주문 상세 API 조회: 주문번호={}, 고객={}", orderNumber, loginUser.getCstmNumber());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("주문 상세 API 조회 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response);

        } catch (Exception e) {
            log.error("주문 상세 API 조회 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "주문 상세 정보를 불러올 수 없습니다.");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 주문 상세 조회
     * URL: GET /order/{orderNumber}
     */
    @GetMapping("/{orderNumber}")
    public String orderDetail(@PathVariable String orderNumber,
                              HttpSession session,
                              Model model) {
        try {
            Customer loginUser = (Customer) session.getAttribute("loginUser");
            if (loginUser == null) {
                return "redirect:/login";
            }

            // 주문 상세 정보 조회
            Order orderDetail = orderService.getOrderDetail(orderNumber, loginUser.getCstmNumber());
            model.addAttribute("orderDetail", orderDetail);

            log.info("주문 상세 조회: 주문번호={}, 고객={}", orderNumber, loginUser.getCstmNumber());
            return "redirect:/order/myorders?highlight=" + orderNumber;

        } catch (IllegalArgumentException e) {
            log.warn("주문 상세 조회 실패: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/mypage/orders?error=notfound";

        } catch (Exception e) {
            log.error("주문 상세 조회 실패: {}", e.getMessage(), e);
            return "redirect:/mypage/orders?error=system";
        }
    }

    /**
     * 주문 취소
     * URL: POST /order/{orderNumber}/cancel
     */
    @PostMapping("/{orderNumber}/cancel")
    public String cancelOrder(@PathVariable String orderNumber,
                              @RequestParam(required = false, defaultValue = "고객 요청") String cancelReason,
                              HttpSession session) {
        try {
            Customer loginUser = (Customer) session.getAttribute("loginUser");
            if (loginUser == null) {
                return "redirect:/login";
            }

            // 주문 취소 처리
            orderService.cancelOrder(orderNumber, loginUser.getCstmNumber(), cancelReason);

            log.info("주문 취소 완료: 주문번호={}, 고객={}, 사유={}",
                    orderNumber, loginUser.getCstmNumber(), cancelReason);

            // 🔥 myorders로 리다이렉트 (성공 메시지와 함께)
            return "redirect:/order/myorders?cancelled=true&orderNumber=" + orderNumber;

        } catch (IllegalArgumentException e) {
            log.warn("주문 취소 실패: {}", e.getMessage());
            // 🔥 myorders로 리다이렉트 (에러 메시지와 함께)
            return "redirect:/order/myorders?error=cancel&message=" +
                    java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("주문 취소 실패: {}", e.getMessage(), e);
            // 🔥 myorders로 리다이렉트 (시스템 에러)
            return "redirect:/order/myorders?error=system";
        }
    }

    /**
     * 고객의 주문 목록 조회 (마이페이지용)
     * URL: GET /order/my-orders
     */
    @GetMapping("/myorders")
    public String myOrders(HttpSession session, Model model) {
        try {
            Customer loginUser = (Customer) session.getAttribute("loginUser");
            if (loginUser == null) {
                return "redirect:/login";
            }

            model.addAttribute("loginName", loginUser.getCstmName());

            // 고객의 주문 목록 조회
            List<Order> orders = orderService.getCustomerOrders(loginUser.getCstmNumber());
            model.addAttribute("orders", orders);

            model.addAttribute("customerName", loginUser.getCstmName());

            return "order/myorders"; // templates/order/myorders.html

        } catch (Exception e) {

            model.addAttribute("errorMessage", "주문 목록을 불러오는 중 오류가 발생했습니다.");
            return "order/myorders";
        }
    }

    /**
     * 주문 상태 변경 (관리자용)
     * URL: POST /order/{orderNumber}/status
     */
    @PostMapping("/{orderNumber}/status")
    public String updateOrderStatus(@PathVariable String orderNumber,
                                    @RequestParam Order.OrderStatus status,
                                    HttpSession session) {
        try {
            // 관리자 권한 확인 (실제로는 관리자 체크 로직 필요)
            Customer loginUser = (Customer) session.getAttribute("loginUser");
            if (loginUser == null) {
                return "redirect:/login";
            }

            // 주문 상태 업데이트
            orderService.updateOrderStatus(orderNumber, status);

            log.info("주문 상태 업데이트: 주문번호={}, 상태={}, 처리자={}",
                    orderNumber, status, loginUser.getCstmNumber());

            return "redirect:/order/" + orderNumber + "?updated=true";

        } catch (Exception e) {
            log.error("주문 상태 업데이트 실패: {}", e.getMessage(), e);
            return "redirect:/order/" + orderNumber + "?error=update";
        }
    }

    @PostMapping("/direct-buy/prepare")
    @ResponseBody
    public ResponseEntity<?> prepareDirectBuy(@RequestParam String productId,
                                              @RequestParam int quantity,
                                              @RequestParam double finalPrice,
                                              @RequestParam(required = false) String karatCode,
                                              HttpSession session) {
        try {
            Customer loginUser = (Customer) session.getAttribute("loginUser");
            if (loginUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "로그인이 필요합니다."));
            }

            // 🔥 간단한 임시 주문 ID 생성
            String tempOrderId = "TEMP_" + System.currentTimeMillis() + "_" + loginUser.getCstmNumber();

            // 🔥 임시 주문 데이터 생성
            Map<String, Object> tempOrderData = new HashMap<>();
            tempOrderData.put("productId", productId);
            tempOrderData.put("quantity", quantity);
            tempOrderData.put("finalPrice", finalPrice);
            tempOrderData.put("karatCode", karatCode);
            tempOrderData.put("customerId", loginUser.getCstmNumber());
            tempOrderData.put("customerName", loginUser.getCstmName());
            tempOrderData.put("customerPhone", loginUser.getCstmPhone());
            tempOrderData.put("customerAddr", loginUser.getCstmAddr());
            tempOrderData.put("createdAt", System.currentTimeMillis());

            // 🔥 세션에 임시 주문 데이터 저장 (30분 후 만료)
            session.setAttribute("tempOrder_" + tempOrderId, tempOrderData);
            session.setMaxInactiveInterval(30 * 60); // 30분

            log.info("바로구매 임시 주문 생성: tempOrderId={}, 고객={}, 상품={}",
                    tempOrderId, loginUser.getCstmNumber(), productId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "tempOrderId", tempOrderId,
                    "message", "임시 주문이 생성되었습니다."
            ));

        } catch (Exception e) {
            log.error("바로구매 임시 주문 생성 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "임시 주문 생성에 실패했습니다."));
        }
    }

    /**
     * 바로구매 주문 페이지
     * URL: GET /order/direct-buy?tempOrderId=TEMP_XXX
     */
    @GetMapping("/direct-buy")
    public String directBuyOrderPage(@RequestParam String tempOrderId,
                                     HttpSession session,
                                     Model model) {
        try {
            Customer loginUser = (Customer) session.getAttribute("loginUser");
            if (loginUser == null) {
                log.warn("로그인되지 않은 사용자의 바로구매 주문 요청");
                return "redirect:/login?returnUrl=/cart";
            }
            model.addAttribute("loginName", loginUser.getCstmName());

            //  간단하게 세션에서 데이터 가져오기
            Object sessionData = session.getAttribute("tempOrder_" + tempOrderId);
            if (sessionData == null) {
                log.warn("임시 주문 데이터 없음 또는 만료: tempOrderId={}", tempOrderId);
                return "redirect:/cart?error=expired";
            }

            //  간단한 캐스팅 (경고 무시)
            @SuppressWarnings("unchecked")
            Map<String, Object> tempOrderData = (Map<String, Object>) sessionData;

            //  임시 주문 정보를 CartDTO 형태로 변환 (기존 메서드 사용)
            List<CartDTO> orderItems = orderService.createDirectBuyCartItems(tempOrderData);

            //  총 금액 계산
            double totalAmount = (Double) tempOrderData.get("finalPrice") * (Integer) tempOrderData.get("quantity");
            int totalQuantity = (Integer) tempOrderData.get("quantity");

            //  모델에 데이터 설정
            model.addAttribute("cartItems", orderItems);
            model.addAttribute("totalAmount", totalAmount);
            model.addAttribute("totalQuantity", totalQuantity);
            model.addAttribute("customerName", loginUser.getCstmName());
            model.addAttribute("customerPhone", loginUser.getCstmPhone());
            model.addAttribute("customerAddr", loginUser.getCstmAddr());
            model.addAttribute("tempOrderId", tempOrderId);
            model.addAttribute("isDirectBuy", true);

            log.info("바로구매 주문 페이지 요청: tempOrderId={}, 고객={}", tempOrderId, loginUser.getCstmNumber());

            return "order/order";

        } catch (Exception e) {
            log.error("바로구매 주문 페이지 요청 실패: {}", e.getMessage(), e);
            return "redirect:/cart?error=system";
        }
    }

    /**
     * 바로구매 주문 생성 (결제 페이지로 이동)
     * URL: POST /order/direct-buy/create
     */
    @PostMapping("/direct-buy/create")
    public String createDirectBuyOrder(@RequestParam String tempOrderId,
                                       @ModelAttribute OrderCreateRequest orderRequest,
                                       HttpSession session) {
        try {
            Customer loginUser = (Customer) session.getAttribute("loginUser");
            if (loginUser == null) {
                return "redirect:/login?returnUrl=/cart";
            }

            // 🔥 간단하게 세션에서 데이터 가져오기
            Object sessionData = session.getAttribute("tempOrder_" + tempOrderId);
            if (sessionData == null) {
                log.warn("임시 주문 데이터 없음: tempOrderId={}", tempOrderId);
                return "redirect:/cart?error=expired";
            }

            // 🔥 간단한 캐스팅 (경고 무시)
            @SuppressWarnings("unchecked")
            Map<String, Object> tempOrderData = (Map<String, Object>) sessionData;

            String cstmNumber = loginUser.getCstmNumber();
            orderRequest.setCstmNumber(cstmNumber);

            // 🔥 기존 OrderService 메서드 사용
            String orderNumber = orderService.createDirectBuyOrder(tempOrderData, orderRequest);

            // 🔥 임시 주문 데이터 삭제 (사용 완료)
            session.removeAttribute("tempOrder_" + tempOrderId);

            log.info("바로구매 주문 생성 완료: 주문번호={}, tempOrderId={}", orderNumber, tempOrderId);

            return "redirect:/payment/request?orderNumber=" + orderNumber;

        } catch (OrderService.StockShortageException e) {
            log.error("❌ 재고 부족 예외: {}", e.getMessage());
            return "redirect:/order/direct-buy?tempOrderId=" + tempOrderId + "&error=stock";
        } catch (Exception e) {
            log.error("바로구매 주문 생성 실패: {}", e.getMessage(), e);
            return "redirect:/order/direct-buy?tempOrderId=" + tempOrderId + "&error=create";
        }
    }

    /**
     * 결제 실패 시 장바구니 추가 API
     * URL: POST /order/payment-failed/add-to-cart
     */
    @PostMapping("/payment-failed/add-to-cart")
    @ResponseBody
    public ResponseEntity<?> addFailedPaymentToCart(@RequestParam String orderNumber,
                                                    HttpSession session) {
        try {
            Customer loginUser = (Customer) session.getAttribute("loginUser");
            if (loginUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "로그인이 필요합니다."));
            }

            // 🔥 주문을 장바구니에 추가하고 주문 취소
            boolean result = orderService.moveOrderToCart(orderNumber, loginUser.getCstmNumber());

            if (result) {
                log.info("결제 실패 상품 장바구니 추가 완료: 주문번호={}, 고객={}", orderNumber, loginUser.getCstmNumber());

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "상품이 장바구니에 추가되었습니다."
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "장바구니 추가에 실패했습니다."
                ));
            }

        } catch (Exception e) {
            log.error("결제 실패 장바구니 추가 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "장바구니 추가에 실패했습니다."));
        }
    }


}