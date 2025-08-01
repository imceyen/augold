package com.global.augold.order.service;

import com.global.augold.cart.dto.CartDTO;
import com.global.augold.cart.entity.Cart;
import com.global.augold.cart.repository.CartRepository;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    @Lazy
    private final CartService cartService;


    @Autowired
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
     * 장바구니에서 주문 생성 - cartService 대신 직접 구현
     */
    public String createOrderFromCart(OrderCreateRequest orderRequest, String cstmNumber) {
        try {
            // 🔥 cartService.getCartList() 대신 직접 구현
            List<CartDTO> allCartItems = getCartListFromRepository(cstmNumber);
            if (allCartItems.isEmpty()) {
                throw new IllegalArgumentException("장바구니가 비어있습니다.");
            }

            // 선택된 상품만 필터링
            List<CartDTO> cartItems;
            String selectedProductIds = orderRequest.getSelectedProductIds();

            if (selectedProductIds != null && !selectedProductIds.isEmpty()) {
                List<String> selectedKeys = Arrays.asList(selectedProductIds.split(","));
                cartItems = allCartItems.stream()
                        .filter(item -> {
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

            // 재고 검증
            validateStock(cartItems);

            // 주문 생성
            Order order = createOrder(orderRequest, cstmNumber, cartItems);
            orderRepository.save(order);

            // 방금 저장한 고객의 최신 주문 조회
            List<Order> recentOrders = orderRepository.findByCstmNumberOrderByOrderDateDesc(cstmNumber);
            if (recentOrders.isEmpty()) {
                throw new RuntimeException("주문 저장 후 조회 실패");
            }

            String actualOrderNumber = recentOrders.get(0).getOrderNumber();

            // 주문 상품 생성
            createOrderItems(actualOrderNumber, cartItems);

            return actualOrderNumber;

        } catch (Exception e) {
            log.error("주문 생성 실패: 고객={}, 오류={}", cstmNumber, e.getMessage(), e);
            throw new RuntimeException("주문 생성에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 🔥 CartService.getCartList() 대신 직접 구현
     */
    private List<CartDTO> getCartListFromRepository(String cstmNumber) {
        try {
            // Cart 엔티티 조회
            List<Cart> carts = cartRepository.findByCstmNumberOrderByCartDateDesc(cstmNumber);

            List<CartDTO> cartDTOs = new ArrayList<>();

            for (Cart cart : carts) {
                // Product 정보 조회
                Product product = productRepository.findByProductId(cart.getProductId())
                        .orElse(null);

                if (product != null) {
                    CartDTO cartDTO = new CartDTO();
                    cartDTO.setProductId(cart.getProductId());
                    cartDTO.setQuantity(cart.getQuantity());
                    cartDTO.setKaratName(cart.getKaratCode() != null ? cart.getKaratCode() : "");
                    cartDTO.setProductName(product.getProductName());
                    cartDTO.setImageUrl(product.getImageUrl());
                    cartDTO.setFinalPrice(product.getFinalPrice()); // Product에서 가격 가져오기

                    cartDTOs.add(cartDTO);
                }
            }

            return cartDTOs;

        } catch (Exception e) {
            log.error("장바구니 목록 조회 실패: 고객={}, 오류={}", cstmNumber, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 바로 구매 (장바구니 거치지 않고 직접 주문 생성)
     */
    public String createDirectOrder(String productId, Integer quantity, String cstmNumber) {
        try {
            // 상품 정보 조회
            Product product = productRepository.findByProductId(productId)
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));

            // 재고 검증
            if (product.getProductInventory() < quantity) {
                if (product.getProductInventory() == 0) {
                    throw new StockShortageException(product.getProductName(), product.getProductId(), quantity, 0);
                } else {
                    throw new StockShortageException(product.getProductName(), product.getProductId(),
                            quantity, product.getProductInventory());
                }
            }

            // 주문 생성
            Order order = new Order();
            order.setOrderNumber("");
            order.setCstmNumber(cstmNumber);

            // 금액 계산
            BigDecimal unitPrice = BigDecimal.valueOf(product.getFinalPrice());
            BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
            order.setTotalAmount(totalAmount);
            order.setFinalAmount(totalAmount);

            order.setDeliveryAddr("");
            order.setDeliveryPhone("");

            orderRepository.save(order);

            // 최신 주문 조회
            List<Order> recentOrders = orderRepository.findByCstmNumberOrderByOrderDateDesc(cstmNumber);
            if (recentOrders.isEmpty()) {
                throw new RuntimeException("주문 저장 후 조회 실패");
            }

            String actualOrderNumber = recentOrders.get(0).getOrderNumber();

            // 주문 상품 생성
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderNumber(actualOrderNumber);
            orderItem.setProductId(product.getProductId());
            orderItem.setQuantity(quantity);
            orderItem.setUnitPrice(unitPrice);
            orderItem.setFinalAmount(totalAmount);
            orderItem.setOrderItemId("");

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

        order.setOrderNumber("");
        order.setCstmNumber(cstmNumber);

        // 금액 계산
        BigDecimal totalAmount = calculateTotalAmount(cartItems);
        order.setTotalAmount(totalAmount);
        order.setFinalAmount(totalAmount);

        // 배송 정보
        order.setDeliveryAddr(orderRequest.getDeliveryAddr());
        order.setDeliveryPhone(orderRequest.getDeliveryPhone());

        return order;
    }

    /**
     * 주문 상품 생성
     */
    private void createOrderItems(String orderNumber, List<CartDTO> cartItems) {
        for (CartDTO cartItem : cartItems) {
            Product actualProduct = findActualProduct(cartItem.getProductId(), cartItem.getKaratName());

            if (actualProduct == null) {
                throw new IllegalArgumentException("상품을 찾을 수 없습니다: " + cartItem.getProductId() + ":" + cartItem.getKaratName());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderNumber(orderNumber);
            orderItem.setProductId(actualProduct.getProductId());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(BigDecimal.valueOf(cartItem.getFinalPrice()));
            orderItem.setFinalAmount(BigDecimal.valueOf(cartItem.getFinalPrice() * cartItem.getQuantity()));
            orderItem.setOrderItemId("");

            orderItemRepository.save(orderItem);
            entityManager.flush();
            entityManager.clear();
        }
    }

    private Product findActualProduct(String cartProductId, String karatCode) {
        try {
            Optional<Product> baseProduct = productRepository.findById(cartProductId);

            if (baseProduct.isPresent() && baseProduct.get().getProductGroup() != null) {
                List<Product> groupProducts = productRepository.findByProductGroup(baseProduct.get().getProductGroup());

                Optional<Product> correctProduct = groupProducts.stream()
                        .filter(p -> karatCode.equals(p.getKaratCode()))
                        .findFirst();

                if (correctProduct.isPresent()) {
                    return correctProduct.get();
                }
            }

            return baseProduct.orElse(null);
        } catch (Exception e) {
            return null;
        }
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

            if (!order.getCstmNumber().equals(cstmNumber)) {
                throw new IllegalArgumentException("본인의 주문만 취소할 수 있습니다.");
            }

            if (!order.isCancellable()) {
                throw new IllegalArgumentException("취소할 수 없는 주문 상태입니다: " + order.getOrderStatus());
            }

            order.cancelOrder();
            orderRepository.save(order);

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

        if (!order.getCstmNumber().equals(cstmNumber)) {
            throw new IllegalArgumentException("본인의 주문만 조회할 수 있습니다.");
        }

        enrichOrderItemsWithProductInfo(order);

        return order;
    }

    /**
     * 고객의 주문 목록 조회 (상품 정보 포함)
     */
    public List<Order> getCustomerOrders(String cstmNumber) {
        List<Order> orders = orderRepository.findByCstmNumberAndOrderStatusNotOrderByOrderDateDesc(
                cstmNumber, Order.OrderStatus.PENDING);

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
                Product product = productRepository.findByProductId(orderItem.getProductId())
                        .orElse(null);

                if (product != null) {
                    orderItem.setProductName(product.getProductName());
                    orderItem.setImageUrl(product.getImageUrl());

                    log.debug("OrderItem에 Product 정보 설정: {} -> {}",
                            orderItem.getProductId(), product.getProductName());
                } else {
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

    /**
     * 임시 주문 데이터를 CartDTO로 변환 (주문서 페이지에서 사용)
     */
    public List<CartDTO> createDirectBuyCartItems(Map<String, Object> tempOrderData) {
        try {
            String productId = (String) tempOrderData.get("productId");
            int quantity = (Integer) tempOrderData.get("quantity");
            double finalPrice = (Double) tempOrderData.get("finalPrice");
            String karatCode = (String) tempOrderData.get("karatCode");

            Product product = productRepository.findByProductId(productId)
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));

            CartDTO cartItem = new CartDTO();
            cartItem.setProductId(productId);
            cartItem.setQuantity(quantity);
            cartItem.setFinalPrice(finalPrice);
            cartItem.setKaratName(karatCode != null ? karatCode : "");
            cartItem.setProductName(product.getProductName());
            cartItem.setImageUrl(product.getImageUrl());

            List<CartDTO> orderItems = new ArrayList<>();
            orderItems.add(cartItem);

            log.info("바로구매 CartDTO 생성: 상품={}, 수량={}", product.getProductName(), quantity);

            return orderItems;

        } catch (Exception e) {
            log.error("바로구매 CartDTO 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("주문 상품 정보 생성에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 바로구매 주문 생성 (임시 데이터 → 실제 주문)
     */
    public String createDirectBuyOrder(Map<String, Object> tempOrderData, OrderCreateRequest orderRequest) {
        String cstmNumber = orderRequest.getCstmNumber();

        try {
            String productId = (String) tempOrderData.get("productId");
            int quantity = (Integer) tempOrderData.get("quantity");
            double finalPrice = (Double) tempOrderData.get("finalPrice");
            String karatCode = (String) tempOrderData.get("karatCode");

            Product product = productRepository.findByProductId(productId)
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));

            if (product.getProductInventory() < quantity) {
                throw new StockShortageException(product.getProductName(), product.getProductId(),
                        quantity, product.getProductInventory());
            }

            Order order = new Order();
            order.setOrderNumber("");
            order.setCstmNumber(cstmNumber);
            order.setOrderStatus(Order.OrderStatus.PENDING);

            order.setDeliveryAddr(orderRequest.getDeliveryAddr());
            order.setDeliveryPhone(orderRequest.getDeliveryPhone());

            BigDecimal totalAmount = BigDecimal.valueOf(finalPrice * quantity);
            order.setTotalAmount(totalAmount);
            order.setFinalAmount(totalAmount);

            orderRepository.save(order);

            List<Order> recentOrders = orderRepository.findByCstmNumberOrderByOrderDateDesc(cstmNumber);
            if (recentOrders.isEmpty()) {
                throw new RuntimeException("주문 저장 후 조회 실패");
            }

            String actualOrderNumber = recentOrders.get(0).getOrderNumber();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderNumber(actualOrderNumber);
            orderItem.setProductId(productId);
            orderItem.setQuantity(quantity);
            orderItem.setUnitPrice(BigDecimal.valueOf(finalPrice));
            orderItem.setFinalAmount(BigDecimal.valueOf(finalPrice * quantity));
            orderItem.setOrderItemId("");

            orderItemRepository.save(orderItem);
            entityManager.flush();
            entityManager.clear();

            log.info("바로구매 주문 생성 완료: 주문번호={}, 고객={}, 상품={}, 수량={}, 금액={}",
                    actualOrderNumber, cstmNumber, productId, quantity, totalAmount);

            return actualOrderNumber;

        } catch (StockShortageException e) {
            throw e;
        } catch (Exception e) {
            log.error("바로구매 주문 생성 실패: 고객={}, 오류={}", cstmNumber, e.getMessage(), e);
            throw new RuntimeException("주문 생성에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 🔥 결제 실패한 주문을 장바구니로 이동 - Cart 엔티티 구조에 맞게 수정
     */
    public boolean moveOrderToCart(String orderNumber, String cstmNumber) {
        try {
            Order order = orderRepository.findByOrderNumber(orderNumber)
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderNumber));

            if (!order.getCstmNumber().equals(cstmNumber)) {
                throw new IllegalArgumentException("본인의 주문만 처리할 수 있습니다.");
            }

            if (order.getOrderStatus() != Order.OrderStatus.PENDING) {
                throw new IllegalArgumentException("처리할 수 없는 주문 상태입니다: " + order.getOrderStatus());
            }

            List<OrderItem> orderItems = orderItemRepository.findByOrderNumber(orderNumber);

            for (OrderItem orderItem : orderItems) {
                Product product = productRepository.findByProductId(orderItem.getProductId())
                        .orElse(null);

                if (product != null) {
                    // 🔥 Cart 엔티티 구조에 맞게 수정
                    Cart cart = new Cart();
                    cart.setCartNumber(""); // 트리거에서 자동 생성
                    cart.setCstmNumber(cstmNumber);
                    cart.setProductId(orderItem.getProductId());
                    cart.setQuantity(orderItem.getQuantity());
                    cart.setKaratCode(product.getKaratCode()); // Product에서 karatCode 가져오기
                    cart.setCartDate(LocalDateTime.now());

                    cartRepository.save(cart);

                    log.info("주문 상품 장바구니 추가: 주문번호={}, 상품={}, 수량={}",
                            orderNumber, product.getProductName(), orderItem.getQuantity());
                }
            }

            order.cancelOrder();
            orderRepository.save(order);

            log.info("결제 실패 주문 장바구니 이동 완료: 주문번호={}, 고객={}", orderNumber, cstmNumber);

            return true;

        } catch (Exception e) {
            log.error("주문 장바구니 이동 실패: 주문번호={}, 오류={}", orderNumber, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 주문이 바로구매인지 확인 (상품 개수로 판단)
     */
    public boolean isDirectBuyOrder(String orderNumber) {
        try {
            List<OrderItem> orderItems = orderItemRepository.findByOrderNumber(orderNumber);
            return orderItems.size() == 1;

        } catch (Exception e) {
            log.error("바로구매 주문 확인 실패: 주문번호={}", orderNumber);
            return false;
        }
    }

    /**
     * 결제 실패한 주문 취소 (장바구니 추가 없이)
     */
    public void cancelFailedOrder(String orderNumber, String cstmNumber) {
        try {
            Order order = orderRepository.findByOrderNumber(orderNumber)
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderNumber));

            if (!order.getCstmNumber().equals(cstmNumber)) {
                throw new IllegalArgumentException("본인의 주문만 처리할 수 있습니다.");
            }

            order.cancelOrder();
            orderRepository.save(order);

            log.info("결제 실패 주문 취소: 주문번호={}, 고객={}", orderNumber, cstmNumber);

        } catch (Exception e) {
            log.error("결제 실패 주문 취소 실패: 주문번호={}, 오류={}", orderNumber, e.getMessage(), e);
            throw new RuntimeException("주문 취소에 실패했습니다: " + e.getMessage());
        }
    }
    
}