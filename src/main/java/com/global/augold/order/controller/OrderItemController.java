package com.global.augold.order.controller;

import com.global.augold.member.entity.Customer;
import com.global.augold.order.dto.OrderItemDTO;
import com.global.augold.order.service.OrderItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/order-item")
public class OrderItemController {

    private final OrderItemService orderItemService;

    /**
     * 주문 상품 목록 조회 페이지
     * URL: GET /order-item/list/{orderNumber}
     */
    @GetMapping("/list/{orderNumber}")
    public String orderItemList(@PathVariable String orderNumber,
                                HttpSession session,
                                Model model) {
        try {
            Customer loginUser = (Customer) session.getAttribute("loginUser");
            if (loginUser == null) {
                return "redirect:/login";
            }

            // 주문 상품 목록 조회
            List<OrderItemDTO> orderItems = orderItemService.getOrderItemsByOrderNumber(orderNumber);

            // 주문 요약 정보
            Integer totalQuantity = orderItemService.getTotalQuantityByOrder(orderNumber);

            model.addAttribute("orderNumber", orderNumber);
            model.addAttribute("orderItems", orderItems);
            model.addAttribute("totalQuantity", totalQuantity);

            log.info("주문 상품 목록 조회: orderNumber={}, 상품수={}", orderNumber, orderItems.size());
            return "order/order-item-list"; // templates/order/order-item-list.html

        } catch (Exception e) {
            log.error("주문 상품 목록 조회 실패: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "주문 상품 목록을 불러오는 중 오류가 발생했습니다.");
            return "error/error-page";
        }
    }

    /**
     * 주문 상품 상세 조회 페이지
     * URL: GET /order-item/detail/{orderNumber}/{productId}
     */
    @GetMapping("/detail/{orderNumber}/{productId}")
    public String orderItemDetail(@PathVariable String orderNumber,
                                  @PathVariable String productId,
                                  HttpSession session,
                                  Model model) {
        try {
            Customer loginUser = (Customer) session.getAttribute("loginUser");
            if (loginUser == null) {
                return "redirect:/login";
            }

            // 주문 상품 상세 조회
            OrderItemDTO orderItem = orderItemService.getOrderItemDetail(orderNumber, productId);

            model.addAttribute("orderItem", orderItem);

            log.info("주문 상품 상세 조회: orderNumber={}, productId={}", orderNumber, productId);
            return "order/order-item-detail"; // templates/order/order-item-detail.html

        } catch (IllegalArgumentException e) {
            log.warn("주문 상품 상세 조회 실패: {}", e.getMessage());
            return "redirect:/order/" + orderNumber + "?error=item_not_found";

        } catch (Exception e) {
            log.error("주문 상품 상세 조회 실패: {}", e.getMessage(), e);
            return "redirect:/order/" + orderNumber + "?error=system";
        }
    }

    // ===============================
    // REST API 엔드포인트들
    // ===============================

    /**
     * 주문 상품 목록 조회 API
     * URL: GET /api/order-item/list/{orderNumber}
     */
    @GetMapping("/api/order-item/list/{orderNumber}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOrderItemsApi(@PathVariable String orderNumber,
                                                                HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        try {
            Customer loginUser = (Customer) session.getAttribute("loginUser");
            if (loginUser == null) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(response);
            }

            // 주문 상품 목록 조회
            List<OrderItemDTO> orderItems = orderItemService.getOrderItemsByOrderNumber(orderNumber);
            Integer totalQuantity = orderItemService.getTotalQuantityByOrder(orderNumber);

            response.put("success", true);
            response.put("orderItems", orderItems);
            response.put("totalQuantity", totalQuantity);
            response.put("itemCount", orderItems.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("주문 상품 목록 API 조회 실패: {}", e.getMessage(), e);

            response.put("success", false);
            response.put("message", "주문 상품 목록 조회에 실패했습니다.");

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 상품별 주문 내역 조회 API
     * URL: GET /api/order-item/product/{productId}
     */
    @GetMapping("/api/order-item/product/{productId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getProductOrderHistoryApi(@PathVariable String productId,
                                                                         HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        try {
            Customer loginUser = (Customer) session.getAttribute("loginUser");
            if (loginUser == null) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(response);
            }

            // 상품별 주문 내역 조회
            List<OrderItemDTO> orderItems = orderItemService.getOrderItemsByProductId(productId);
            Integer totalSold = orderItemService.getTotalSoldQuantityByProduct(productId);

            response.put("success", true);
            response.put("orderItems", orderItems);
            response.put("totalSold", totalSold);
            response.put("orderCount", orderItems.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("상품별 주문 내역 API 조회 실패: {}", e.getMessage(), e);

            response.put("success", false);
            response.put("message", "상품별 주문 내역 조회에 실패했습니다.");

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 인기 상품 조회 API
     * URL: GET /api/order-item/top-selling
     */
    @GetMapping("/api/order-item/top-selling")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTopSellingProductsApi(@RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 인기 상품 조회
            List<OrderItemDTO> topProducts = orderItemService.getTopSellingProducts(limit);

            response.put("success", true);
            response.put("topProducts", topProducts);
            response.put("count", topProducts.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("인기 상품 조회 API 실패: {}", e.getMessage(), e);

            response.put("success", false);
            response.put("message", "인기 상품 조회에 실패했습니다.");

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 주문 상품 통계 API
     * URL: GET /api/order-item/stats/{orderNumber}
     */
    @GetMapping("/api/order-item/stats/{orderNumber}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOrderStatsApi(@PathVariable String orderNumber,
                                                                HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        try {
            Customer loginUser = (Customer) session.getAttribute("loginUser");
            if (loginUser == null) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(response);
            }

            // 주문 통계 조회
            Integer totalQuantity = orderItemService.getTotalQuantityByOrder(orderNumber);

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalQuantity", totalQuantity);

            response.put("success", true);
            response.put("stats", stats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("주문 통계 API 조회 실패: {}", e.getMessage(), e);

            response.put("success", false);
            response.put("message", "주문 통계 조회에 실패했습니다.");

            return ResponseEntity.status(500).body(response);
        }
    }
}