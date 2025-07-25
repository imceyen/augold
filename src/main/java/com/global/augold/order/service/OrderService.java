// ===============================
// OrderService.java - 주문 서비스
// ===============================

package com.global.augold.order.service;

import com.global.augold.cart.dto.CartDTO;
import com.global.augold.cart.service.CartService;
import com.global.augold.order.dto.OrderCreateRequest;
import com.global.augold.order.entity.Order;
import com.global.augold.order.entity.OrderItem;
import com.global.augold.order.repository.OrderRepository;
import com.global.augold.order.repository.OrderItemRepository;
import com.global.augold.product.entity.Product;
import com.global.augold.product.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;



import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;

    @Autowired  // 🔥 이 줄 추가
    private EntityManager entityManager;

    // 재고 부족 예외 클래스

    public static class StockShortageException extends IllegalArgumentException {
        private final String productName;
        private final int requestedQuantity;
        private final int availableStock;
        private final String productId;

        public StockShortageException(String productName, String productId, int requestedQuantity, int availableStock) {
            super(String.format("%s의 재고가 부족합니다. 요청: %d개, 재고: %d개", productName, requestedQuantity, availableStock));
            this.productName = productName;
            this.productId = productId;
            this.requestedQuantity = requestedQuantity;
            this.availableStock = availableStock;
        }

        // getter 메서드들
        public String getProductName() { return productName; }
        public String getProductId() { return productId; }
        public int getRequestedQuantity() { return requestedQuantity; }
        public int getAvailableStock() { return availableStock; }
    }

    /**
     * 장바구니에서 주문 생성
     */
    public String createOrderFromCart(OrderCreateRequest orderRequest, String cstmNumber) {
        try {
            // 1. 전체 장바구니 상품 목록 조회
            List<CartDTO> allCartItems = cartService.getCartList(cstmNumber);
            if (allCartItems.isEmpty()) {
                throw new IllegalArgumentException("장바구니가 비어있습니다.");
            }

            //  2. 선택된 상품만 필터링
            List<CartDTO> cartItems;
            String selectedProductIds = orderRequest.getSelectedProductIds();

            if (selectedProductIds != null && !selectedProductIds.isEmpty()) {
                List<String> selectedKeys = Arrays.asList(selectedProductIds.split(","));
                cartItems = allCartItems.stream()
                        .filter(item -> {
                            //  productId:karatName 조합으로 매칭
                            String itemKey = item.getProductId() + ":" + item.getKaratName();
                            return selectedKeys.contains(itemKey);
                        })
                        .collect(Collectors.toList());

                log.info("선택된 상품으로 주문 생성: 전체={}, 선택={}", allCartItems.size(), cartItems.size());
            } else {
                cartItems = allCartItems;
                log.info("전체 상품으로 주문 생성: {}", cartItems.size());
            }

            if (cartItems.isEmpty()) {
                throw new IllegalArgumentException("선택된 상품이 없습니다.");
            }

            // 2. 재고 검증 (선택된 상품만)
            validateStock(cartItems);


            // 3. 주문 생성
            Order order = createOrder(orderRequest, cstmNumber, cartItems);
            Order savedOrder = orderRepository.save(order);

            // 방금 저장한 고객의 최신 주문 조회 (실제 주문번호 얻기)
            List<Order> recentOrders = orderRepository.findByCstmNumberOrderByOrderDateDesc(cstmNumber);
            if (recentOrders.isEmpty()) {
                throw new RuntimeException("주문 저장 후 조회 실패");
            }

            String actualOrderNumber = recentOrders.get(0).getOrderNumber();

            // 4. 주문 상품 생성
            createOrderItems(actualOrderNumber, cartItems);

            return actualOrderNumber;

        } catch (Exception e) {
            log.error("주문 생성 실패: 고객={}, 오류={}", cstmNumber, e.getMessage(), e);
            throw new RuntimeException("주문 생성에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 바로 구매 (장바구니 거치지 않고 직접 주문 생성)
     */
    public String createDirectOrder(String productId, Integer quantity, String cstmNumber) {
        try {
            // 1. 상품 정보 조회
            Product product = productRepository.findByProductId(productId)
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));

            // 2. 재고 검증
            if (product.getProductInventory() < quantity) {
                if (product.getProductInventory() == 0) {
                    throw new StockShortageException(product.getProductName(), product.getProductId(), quantity, 0);
                } else {
                    throw new StockShortageException(product.getProductName(), product.getProductId(),
                            quantity, product.getProductInventory());
                }
            }

            // 3. 주문 생성 (기존 방식과 동일)
            Order order = new Order();
            order.setOrderNumber(""); // 트리거에서 자동 생성
            order.setCstmNumber(cstmNumber);

            // 금액 계산
            BigDecimal unitPrice = BigDecimal.valueOf(product.getFinalPrice());
            BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
            order.setTotalAmount(totalAmount);
            order.setFinalAmount(totalAmount);

            // 배송 정보는 빈 값으로 (주문서에서 입력받음)
            order.setDeliveryAddr("");
            order.setDeliveryPhone("");

            Order savedOrder = orderRepository.save(order);

            // 4. 최신 주문 조회
            List<Order> recentOrders = orderRepository.findByCstmNumberOrderByOrderDateDesc(cstmNumber);
            if (recentOrders.isEmpty()) {
                throw new RuntimeException("주문 저장 후 조회 실패");
            }

            String actualOrderNumber = recentOrders.get(0).getOrderNumber();

            // 5. 주문 상품 생성 (기존 방식과 동일)
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderNumber(actualOrderNumber);
            orderItem.setProductId(product.getProductId());
            orderItem.setQuantity(quantity);
            orderItem.setUnitPrice(unitPrice);
            orderItem.setFinalAmount(totalAmount);
            orderItem.setOrderItemId(""); // 트리거에서 자동 생성

            orderItemRepository.save(orderItem);
            entityManager.flush();
            entityManager.clear();

            log.info("바로 구매 주문 생성 완료: 주문번호={}, 상품={}, 수량={}",
                    actualOrderNumber, product.getProductName(), quantity);

            return actualOrderNumber;

        } catch (StockShortageException e) {
            throw e;
        } catch (Exception e) {
            log.error("바로 구매 주문 생성 실패: 고객={}, 상품={}, 오류={}", cstmNumber, productId, e.getMessage(), e);
            throw new RuntimeException("주문 생성에 실패했습니다: " + e.getMessage());
        }
    }

    // 재고 검증
    private void validateStock(List<CartDTO> cartItems) {
        for (CartDTO cartItem : cartItems) {
            Product product = productRepository.findByProductId(cartItem.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + cartItem.getProductId()));

            if (product.getProductInventory() < cartItem.getQuantity()) {
                if (product.getProductInventory() == 0) {
                    throw new StockShortageException(product.getProductName(), product.getProductId(),
                            cartItem.getQuantity(), 0);
                } else {
                    throw new StockShortageException(product.getProductName(), product.getProductId(),
                            cartItem.getQuantity(), product.getProductInventory());
                }
            }
        }
    }

    /**
     * 주문 엔티티 생성
     */
    private Order createOrder(OrderCreateRequest orderRequest, String cstmNumber, List<CartDTO> cartItems) {
        Order order = new Order();

        order.setOrderNumber(""); //
        order.setCstmNumber(cstmNumber);

        // 금액 계산
        BigDecimal totalAmount = calculateTotalAmount(cartItems);
        order.setTotalAmount(totalAmount);
        order.setFinalAmount(totalAmount); // 할인 등이 없으면 동일

        // 배송 정보
        order.setDeliveryAddr(orderRequest.getDeliveryAddr());
        order.setDeliveryPhone(orderRequest.getDeliveryPhone());

        // 주문 상태는 PENDING으로 설정 (onCreate에서 자동 설정됨)

        return order;
    }

    /**
     * 바로 구매용 주문 엔티티 생성
     */
    private Order createDirectOrderEntity(Product product, Integer quantity, String cstmNumber) {
        Order order = new Order();

        order.setOrderNumber(""); // 트리거에서 자동 생성
        order.setCstmNumber(cstmNumber);

        // 금액 계산
        BigDecimal unitPrice = BigDecimal.valueOf(product.getFinalPrice());
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));

        order.setTotalAmount(totalAmount);
        order.setFinalAmount(totalAmount);

        // 배송 정보는 비워두고 주문서 작성 페이지에서 입력받음
        order.setDeliveryAddr("");
        order.setDeliveryPhone("");

        return order;
    }


    /**
     * 주문 상품 생성
     */
    private void createOrderItems(String orderNumber, List<CartDTO> cartItems) {
        for (CartDTO cartItem : cartItems) {
            // 🔥 실제 K값에 맞는 상품 찾기 (CartService와 동일한 로직)
            Product actualProduct = findActualProduct(cartItem.getProductId(), cartItem.getKaratName());

            if (actualProduct == null) {
                throw new IllegalArgumentException("상품을 찾을 수 없습니다: " + cartItem.getProductId() + ":" + cartItem.getKaratName());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderNumber(orderNumber);
            orderItem.setProductId(actualProduct.getProductId()); // 🔥 실제 상품의 productId 저장
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(BigDecimal.valueOf(cartItem.getFinalPrice()));
            orderItem.setFinalAmount(BigDecimal.valueOf(cartItem.getFinalPrice() * cartItem.getQuantity()));
            orderItem.setOrderItemId("");

            orderItemRepository.save(orderItem);
            entityManager.flush();
            entityManager.clear();
        }
    }

    // 🔥 새로 추가할 메서드
    private Product findActualProduct(String cartProductId, String karatCode) {
        try {
            // CartService의 findCorrectPrice와 동일한 로직
            Optional<Product> baseProduct = productRepository.findById(cartProductId);

            if (baseProduct.isPresent() && baseProduct.get().getProductGroup() != null) {
                List<Product> groupProducts = productRepository.findByProductGroup(baseProduct.get().getProductGroup());

                Optional<Product> correctProduct = groupProducts.stream()
                        .filter(p -> karatCode.equals(p.getKaratCode()))
                        .findFirst();

                if (correctProduct.isPresent()) {
                    return correctProduct.get(); // 실제 K값 상품 반환
                }
            }

            return baseProduct.orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 바로 구매용 주문 상품 생성
     */
    private void createDirectOrderItem(String orderNumber, Product product, Integer quantity) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderNumber(orderNumber);
        orderItem.setProductId(product.getProductId());
        orderItem.setQuantity(quantity);

        BigDecimal unitPrice = BigDecimal.valueOf(product.getFinalPrice());
        orderItem.setUnitPrice(unitPrice);
        orderItem.setFinalAmount(unitPrice.multiply(BigDecimal.valueOf(quantity)));

        orderItem.setOrderItemId(""); // 트리거에서 자동 생성

        orderItemRepository.save(orderItem);
        entityManager.flush();  // DB 즉시 반영
        entityManager.clear();  // 영속성 컨텍스트 비우기

        log.info("바로 구매 주문 상품 생성: 주문번호={}, 상품={}, 수량={}",
                orderNumber, product.getProductName(), quantity);
    }



    /**
     * 총 금액 계산
     */
    private BigDecimal calculateTotalAmount(List<CartDTO> cartItems) {
        double total = cartItems.stream()
                .mapToDouble(item -> item.getFinalPrice() * item.getQuantity())
                .sum();
        return BigDecimal.valueOf(total);
    }


    /**
     * 주문 취소
     */
    public void cancelOrder(String orderNumber, String cstmNumber, String cancelReason) {
        try {
            Order order = orderRepository.findByOrderNumber(orderNumber)
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderNumber));

            // 고객 본인 주문인지 확인
            if (!order.getCstmNumber().equals(cstmNumber)) {
                throw new IllegalArgumentException("본인의 주문만 취소할 수 있습니다.");
            }

            // 취소 가능한 상태인지 확인
            if (!order.isCancellable()) {
                throw new IllegalArgumentException("취소할 수 없는 주문 상태입니다: " + order.getOrderStatus());
            }

            // 주문 취소
            order.cancelOrder();
            orderRepository.save(order);

            // 재고 복원 (결제 완료된 주문의 경우)
            if (order.getOrderStatus() == Order.OrderStatus.PAID) {
                restoreStock(orderNumber);
            }

            log.info("주문 취소 완료: 주문번호={}, 고객={}, 사유={}", orderNumber, cstmNumber, cancelReason);

        } catch (Exception e) {
            log.error("주문 취소 실패: 주문번호={}, 오류={}", orderNumber, e.getMessage(), e);
            throw new RuntimeException("주문 취소에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 재고 복원
     */
    private void restoreStock(String orderNumber) {
        List<OrderItem> orderItems = orderItemRepository.findByOrderNumber(orderNumber);

        for (OrderItem item : orderItems) {
            Product product = productRepository.findByProductId(item.getProductId())
                    .orElse(null);

            if (product != null) {
                product.setProductInventory(product.getProductInventory() + item.getQuantity());
                productRepository.save(product);

                log.info("재고 복원: 상품={}, 복원수량={}, 현재재고={}",
                        product.getProductName(), item.getQuantity(), product.getProductInventory());
            }
        }
    }


    /**
     * 주문 상세 조회 (상품 정보 포함)
     */
    public Order getOrderDetail(String orderNumber, String cstmNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderNumber));

        // 고객 본인 주문인지 확인
        if (!order.getCstmNumber().equals(cstmNumber)) {
            throw new IllegalArgumentException("본인의 주문만 조회할 수 있습니다.");
        }

        // 주문 상품들에 Product 정보 설정
        enrichOrderItemsWithProductInfo(order);

        return order;
    }

    /**
     * 고객의 주문 목록 조회 (상품 정보 포함)
     */
    public List<Order> getCustomerOrders(String cstmNumber) {
        List<Order> orders = orderRepository.findByCstmNumberAndOrderStatusNotOrderByOrderDateDesc(
                cstmNumber, Order.OrderStatus.PENDING);

        // 각 주문의 OrderItem에 Product 정보 설정
        for (Order order : orders) {
            enrichOrderItemsWithProductInfo(order);
        }

        return orders;
    }

    /**
     * OrderItem에 Product 정보 추가하는 헬퍼 메서드
     */
    private void enrichOrderItemsWithProductInfo(Order order) {
        if (order.getOrderItems() != null) {
            for (OrderItem orderItem : order.getOrderItems()) {
                // Product 테이블에서 상품 정보 조회
                Product product = productRepository.findByProductId(orderItem.getProductId())
                        .orElse(null);

                if (product != null) {
                    // OrderItem의 @Transient 필드에 실제 Product 정보 설정
                    orderItem.setProductName(product.getProductName());  // "18K 골드 반지"
                    orderItem.setImageUrl(product.getImageUrl());        // "images/ring1.jpg"

                    log.debug("OrderItem에 Product 정보 설정: {} -> {}",
                            orderItem.getProductId(), product.getProductName());
                } else {
                    // Product를 찾을 수 없는 경우 기본값
                    orderItem.setProductName("상품 정보 없음");
                    orderItem.setImageUrl(null);

                    log.warn("Product를 찾을 수 없음: {}", orderItem.getProductId());
                }
            }
        }
    }


    /**
     * 주문 상태 업데이트
     */
    public void updateOrderStatus(String orderNumber, Order.OrderStatus newStatus) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderNumber));

        order.updateStatus(newStatus);
        orderRepository.save(order);

        log.info("주문 상태 업데이트: 주문번호={}, 상태={}", orderNumber, newStatus);
    }



}