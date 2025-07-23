package com.global.augold.order.controller;

import com.global.augold.cart.dto.CartDTO;
import com.global.augold.cart.service.CartService;
import com.global.augold.member.entity.Customer;
import com.global.augold.order.entity.Order;
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
    public String orderPage(HttpSession session, Model model) {
        try {
            // 세션에서 고객 정보 가져오기
            Customer loginUser = (Customer) session.getAttribute("loginUser");
            if (loginUser == null) {
                log.warn("로그인되지 않은 사용자의 주문 요청");
                return "redirect:/login?returnUrl=/cart";
            }

            String cstmNumber = loginUser.getCstmNumber();

            // 장바구니에서 상품 목록 가져오기
            List<CartDTO> cartItems = cartService.getCartList(cstmNumber);
            if (cartItems.isEmpty()) {
                log.warn("장바구니가 비어있는 상태에서 주문 요청: {}", cstmNumber);
                return "redirect:/cart?error=empty";
            }

            // 총 금액 계산
            double totalAmount = cartItems.stream()
                    .mapToDouble(item -> item.getFinalPrice() * item.getQuantity())
                    .sum();

            // 주문 페이지에 필요한 데이터 설정
            model.addAttribute("cartItems", cartItems);
            model.addAttribute("totalAmount", totalAmount);
            model.addAttribute("customerName", loginUser.getCstmName());
            model.addAttribute("customerPhone", loginUser.getCstmPhone());
            model.addAttribute("customerAddr", loginUser.getCstmAddr());

            log.info("주문 페이지 요청 처리: 고객={}, 상품수={}, 총금액={}",
                    cstmNumber, cartItems.size(), totalAmount);

            return "order/order"; // templates/order/order-form.html

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
                              Model model) {
        try {
            // 세션에서 고객 정보 가져오기
            Customer loginUser = (Customer) session.getAttribute("loginUser");
            if (loginUser == null) {
                log.warn("로그인되지 않은 사용자의 주문 생성 요청");
                return "redirect:/login?returnUrl=/cart";
            }

            String cstmNumber = loginUser.getCstmNumber();
            orderRequest.setCstmNumber(cstmNumber);

            // 주문 생성
            String orderNumber = orderService.createOrderFromCart(orderRequest, cstmNumber);

            // 결제 페이지로 리다이렉트
            return "redirect:/payment/request?orderNumber=" + orderNumber;

        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return orderPage(session, model); // 주문 페이지로 돌아가기

        } catch (Exception e) {
            model.addAttribute("errorMessage", "주문 처리 중 오류가 발생했습니다.");
            return orderPage(session, model);
        }
    }


     // 주문 상세 조회 API (JSON 반환)

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

            // 주문 상세 정보 조회 (기존 메서드 활용)
            Order order = orderService.getOrderDetail(orderNumber, loginUser.getCstmNumber());

            response.put("success", true);
            response.put("order", order);

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
            return "order/order-detail"; // templates/order/order-detail.html

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

            return "redirect:/order/" + orderNumber + "?cancelled=true";

        } catch (IllegalArgumentException e) {
            log.warn("주문 취소 실패: {}", e.getMessage());
            return "redirect:/order/" + orderNumber + "?error=cancel&message=" + e.getMessage();

        } catch (Exception e) {
            log.error("주문 취소 실패: {}", e.getMessage(), e);
            return "redirect:/order/" + orderNumber + "?error=system";
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

            // 고객의 주문 목록 조회
            List<Order> orders = orderService.getCustomerOrders(loginUser.getCstmNumber());
            model.addAttribute("orders", orders);

            log.info("고객 주문 목록 조회: 고객={}, 주문수={}",
                    loginUser.getCstmNumber(), orders.size());

            return "order/myorders"; // templates/order/myorders.html

        } catch (Exception e) {
            log.error("주문 목록 조회 실패: {}", e.getMessage(), e);
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


}