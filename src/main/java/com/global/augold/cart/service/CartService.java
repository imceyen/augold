package com.global.augold.cart.service;

import com.global.augold.cart.dto.CartDTO;
import com.global.augold.cart.entity.Cart;
import com.global.augold.cart.repository.CartRepository;
import com.global.augold.order.service.OrderService;
import com.global.augold.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.global.augold.product.entity.Product;
import com.global.augold.goldPrice.Service.GoldPriceService;
import com.global.augold.product.entity.Product;
import com.global.augold.order.entity.Order;
import com.global.augold.order.entity.OrderItem;
import com.global.augold.order.repository.OrderRepository;
import com.global.augold.order.repository.OrderItemRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional // 클래스 레벨 트랜잭션 추가
public class CartService {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final GoldPriceService goldPriceService;
    @Lazy
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;



    public String addToCart(String cstmNumber, String productId, int quantity, String karatCode, double finalPrice) {

        System.out.println("=== addToCart 호출 ===");
        System.out.println("cstmNumber: " + cstmNumber);
        System.out.println("productId: " + productId);
        System.out.println("quantity: " + quantity);
        System.out.println("karatCode: [" + karatCode + "]"); // 대괄호로 공백 확인
        System.out.println("finalPrice: " + finalPrice);

        // ✅ 기존 아이템 찾기 (같은 상품 + 같은 순도)
        Optional<Cart> existingCart = cartRepository.findByCstmNumberAndProductIdAndKaratCode(cstmNumber, productId, karatCode);



        if (existingCart.isPresent()) {
            // ✅ 기존 아이템이 있으면 수량 증가
            Cart cart = existingCart.get();
            int newQuantity = cart.getQuantity() + quantity;


            cart.setQuantity(newQuantity);
            cart.setCartDate(LocalDateTime.now()); // 날짜 업데이트

            try {
                cartRepository.save(cart);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("장바구니 수량 업데이트 중 오류가 발생했습니다: " + e.getMessage());
            }

        } else {

            Cart cart = new Cart(cstmNumber, productId, quantity, karatCode);

            try {
                cartRepository.save(cart);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("장바구니 저장 중 오류가 발생했습니다: " + e.getMessage());
            }
        }

        return "장바구니에 담겼습니다.";
    }

    // ✅ 수정된 getCartList - groupCartItems 제거 + quantity 처리
    public List<CartDTO> getCartList(String cstmNumber) {
        try {
            List<Object[]> rawResults = cartRepository.findByCstmNumberWithProduct(cstmNumber);
            List<CartDTO> cartItems = new ArrayList<>();

            for (Object[] row : rawResults) {
                String cartProductId = (String) row[1];    // Cart의 product_id
                String karatCode = (String) row[8];        // Cart의 karat_code

                // 🔥 실제 해당 K값의 상품 찾기
                double correctPrice = findCorrectPrice(cartProductId, karatCode);

                CartDTO dto = new CartDTO(
                        (String) row[0],           // cart_number
                        cartProductId,             // product_id
                        ((java.sql.Timestamp) row[2]).toLocalDateTime(), // cart_date
                        (String) row[3],           // cstm_number
                        (String) row[4],           // product_name
                        correctPrice,              // 🔥 올바른 K값별 가격
                        (String) row[6],           // image_url
                        (String) row[7],           // ctgr_id
                        karatCode,                 // karat_code
                        (String) row[9],           // product_group
                        row[10] != null ? ((Number) row[10]).intValue() : 1
                );
                cartItems.add(dto);
            }
            return cartItems;

        } catch (Exception e) {
            // 기존 fallback 코드 그대로 유지
            List<Cart> carts = cartRepository.findByCstmNumberSimple(cstmNumber);
            List<CartDTO> basicItems = carts.stream()
                    .map(cart -> new CartDTO(
                            cart.getCartNumber(),
                            cart.getProductId(),
                            cart.getCartDate(),
                            cart.getCstmNumber()
                    ))
                    .toList();
            return basicItems;
        }
    }

    // 🔥 새로 추가할 메서드
    private double findCorrectPrice(String cartProductId, String karatCode) {
        try {
            Optional<Product> baseProduct = productRepository.findById(cartProductId);

            if (baseProduct.isPresent()) { // Product 존재 확인
                Product product = baseProduct.get(); // product 변수 선언

                // 🔥 골드바인 경우 실시간 가격 계산
                if (goldPriceService.isGoldBar(product.getCtgrId())) {
                    return goldPriceService.calculateGoldBarPrice(product.getGoldWeight());
                }

                // 🔥 주얼리인 경우 기존 로직
                if (product.getProductGroup() != null) {
                    List<Product> groupProducts = productRepository.findByProductGroup(product.getProductGroup());

                    Optional<Product> correctProduct = groupProducts.stream()
                            .filter(p -> karatCode.equals(p.getKaratCode()))
                            .findFirst();

                    if (correctProduct.isPresent()) {
                        return correctProduct.get().getFinalPrice();
                    }
                }

                // 🔥 기본값 반환
                return product.getFinalPrice();
            }

            return 0.0; // Product가 없는 경우

        } catch (Exception e) {
            return 0.0;
        }
    }

    // ✅ 새로운 decreaseQuantity (karatCode 포함)
    public boolean decreaseQuantity(String cstmNumber, String productId, String karatCode) {
        Optional<Cart> cart = cartRepository.findByCstmNumberAndProductIdAndKaratCode(cstmNumber, productId, karatCode);
        if (cart.isPresent()) {
            Cart existingCart = cart.get();
            if (existingCart.getQuantity() > 1) {
                // 수량이 1보다 크면 1 감소
                existingCart.setQuantity(existingCart.getQuantity() - 1);
                existingCart.setCartDate(LocalDateTime.now());
                cartRepository.save(existingCart);
            } else {
                // 수량이 1이면 아예 삭제
                cartRepository.delete(existingCart);
            }
            return true;
        }
        return false;
    }

    // ✅ 순도별 수량 확인 메서드
    public int getQuantityByKarat(String cstmNumber, String productId, String karatCode) {
        Optional<Cart> cart = cartRepository.findByCstmNumberAndProductIdAndKaratCode(cstmNumber, productId, karatCode);
        return cart.map(Cart::getQuantity).orElse(0);
    }

    // ✅ 유지되는 메서드들 (변경 없음)
    public int getCartCount(String cstmNumber) {
        return cartRepository.countByCstmNumber(cstmNumber);
    }

    public boolean deleteCartItems(String cstmNumber, List<String> productIds) {
        try {
            int totalDelete = 0;
            for (String productId : productIds) {
                int deleted = cartRepository.deleteByCstmNumberAndProductId(cstmNumber, productId);
                totalDelete += deleted;
            }
            return totalDelete == productIds.size();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteAllItems(String cstmNumber) {
        try {
            int deletedCount = cartRepository.deleteByCstmNumber(cstmNumber);
            return deletedCount > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteCartItemsWithKarat(String cstmNumber, List<String> productIds, List<String> karatCodes) {
        try {
            int totalDelete = 0;
            for (int i = 0; i < productIds.size(); i++) {
                String productId = productIds.get(i);
                String karatCode = i < karatCodes.size() ? karatCodes.get(i) : null;

                // 🔥 productId + karatCode 조합으로 삭제
                int deleted = cartRepository.deleteByCstmNumberAndProductIdAndKaratCode(cstmNumber, productId, karatCode);
                totalDelete += deleted;
            }
            return totalDelete > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean addOrderItemsToCart(String orderNumber, String cstmNumber) {
        try {
            // OrderService에 위임하여 주문을 장바구니로 이동
            return orderService.moveOrderToCart(orderNumber, cstmNumber);

        } catch (Exception e) {
            return false;
        }
    }

    public String addToCartForDirectBuy(String cstmNumber, String productId, int quantity, String karatCode) {
        try {


            // 상품 정보 조회
            Product product = productRepository.findByProductId(productId)
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));

            // 재고 검증
            if (product.getProductInventory() < quantity) {
                if (product.getProductInventory() == 0) {
                    throw new RuntimeException("품절된 상품입니다: " + product.getProductName());
                } else {
                    throw new RuntimeException(String.format("재고가 부족합니다. 상품: %s, 요청: %d개, 재고: %d개",
                            product.getProductName(), quantity, product.getProductInventory()));
                }
            }

            // 🔥 기존 장바구니에 같은 상품(순도별)이 있는지 확인
            Optional<Cart> existingCart = cartRepository.findByCstmNumberAndProductIdAndKaratCode(
                    cstmNumber, productId, karatCode != null ? karatCode : "");

            if (existingCart.isPresent()) {
                // 🔥 기존 상품이 있으면 수량 증가
                Cart cart = existingCart.get();
                int originalQuantity = cart.getQuantity();
                int newQuantity = originalQuantity + quantity;

                // 최종 수량도 재고 확인
                if (product.getProductInventory() < newQuantity) {
                    throw new RuntimeException(String.format("재고가 부족합니다. 상품: %s, 총 요청: %d개, 재고: %d개",
                            product.getProductName(), newQuantity, product.getProductInventory()));
                }

                cart.setQuantity(newQuantity);
                cart.setCartDate(LocalDateTime.now());
                cartRepository.save(cart);



                return "기존 상품 수량이 증가되었습니다.";

            } else {
                // 🔥 새로운 상품 추가
                Cart newCart = new Cart(cstmNumber, productId, quantity, karatCode);
                cartRepository.save(newCart);



                return "상품이 장바구니에 추가되었습니다.";
            }

        } catch (RuntimeException e) {

            throw e; // RuntimeException 그대로 던짐
        } catch (Exception e) {

            throw new RuntimeException("장바구니 추가에 실패했습니다: " + e.getMessage());
        }
    }


    public void reduceCartQuantityAfterDirectBuyPayment(String orderNumber) {
        try {
            Order order = orderRepository.findByOrderNumber(orderNumber)
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderNumber));

            List<OrderItem> orderItems = orderItemRepository.findByOrderNumber(orderNumber);
            String cstmNumber = order.getCstmNumber();

            for (OrderItem orderItem : orderItems) {
                String productId = orderItem.getProductId();
                int orderedQuantity = orderItem.getQuantity();

                // 🔥 상품 정보로 karatCode 찾기
                Product product = productRepository.findByProductId(productId).orElse(null);
                String karatCode = product != null ? (product.getKaratCode() != null ? product.getKaratCode() : "") : "";

                // 🔥 해당 고객의 장바구니에서 같은 상품 찾기 (karatCode까지 매칭)
                Optional<Cart> cartOptional = cartRepository.findByCstmNumberAndProductIdAndKaratCode(
                        cstmNumber, productId, karatCode);

                if (cartOptional.isPresent()) {
                    Cart cart = cartOptional.get();

                    if (cart.getQuantity() > orderedQuantity) {
                        // 🔥 장바구니 수량이 더 많으면 차감만
                        int originalQuantity = cart.getQuantity();
                        cart.setQuantity(originalQuantity - orderedQuantity);
                        cartRepository.save(cart);


                    } else if (cart.getQuantity() == orderedQuantity) {
                        // 🔥 수량이 정확히 같으면 삭제
                        cartRepository.delete(cart);


                    } else {
                        // 🔥 장바구니 수량이 주문 수량보다 적은 경우 (전체 삭제)


                        cartRepository.delete(cart);
                    }
                } else {

                }
            }


        } catch (Exception e) {

            // 에러가 나더라도 결제는 완료된 상태이므로 예외를 던지지 않음
        }
    }

    /**
     * 🔥 일반 주문 결제 완료 시 장바구니 처리 (선택된 상품들만)
     */
    public void reduceCartQuantityAfterPayment(String orderNumber) {
        try {
            Order order = orderRepository.findByOrderNumber(orderNumber)
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderNumber));

            List<OrderItem> orderItems = orderItemRepository.findByOrderNumber(orderNumber);
            String cstmNumber = order.getCstmNumber();

            for (OrderItem orderItem : orderItems) {
                String productId = orderItem.getProductId();
                int orderedQuantity = orderItem.getQuantity();

                // 🔥 상품 정보로 karatCode 찾기
                Product product = productRepository.findByProductId(productId).orElse(null);
                String karatCode = product != null ? (product.getKaratCode() != null ? product.getKaratCode() : "") : "";

                // 🔥 장바구니에서 해당 상품 찾아서 차감/삭제
                Optional<Cart> cartOptional = cartRepository.findByCstmNumberAndProductIdAndKaratCode(
                        cstmNumber, productId, karatCode);

                if (cartOptional.isPresent()) {
                    Cart cart = cartOptional.get();

                    if (cart.getQuantity() > orderedQuantity) {
                        cart.setQuantity(cart.getQuantity() - orderedQuantity);
                        cartRepository.save(cart);


                    } else {
                        cartRepository.delete(cart);

                    }
                }
            }



        } catch (Exception e) {

        }
    }

    /**
     * 🔥 주문 취소 시 장바구니에 상품 복원 (기존 상품이 있으면 수량 증가)
     */
    public boolean restoreOrderToCart(String orderNumber, String cstmNumber) {
        try {
            Order order = orderRepository.findByOrderNumber(orderNumber)
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderNumber));

            if (!order.getCstmNumber().equals(cstmNumber)) {
                throw new IllegalArgumentException("본인의 주문만 처리할 수 있습니다.");
            }

            List<OrderItem> orderItems = orderItemRepository.findByOrderNumber(orderNumber);

            for (OrderItem orderItem : orderItems) {
                Product product = productRepository.findByProductId(orderItem.getProductId())
                        .orElse(null);

                if (product != null) {
                    String karatCode = product.getKaratCode() != null ? product.getKaratCode() : "";

                    // 🔥 기존 장바구니에 같은 상품이 있는지 확인
                    Optional<Cart> existingCart = cartRepository.findByCstmNumberAndProductIdAndKaratCode(
                            cstmNumber, orderItem.getProductId(), karatCode);

                    if (existingCart.isPresent()) {
                        // 🔥 기존 상품이 있으면 수량 증가
                        Cart cart = existingCart.get();
                        cart.setQuantity(cart.getQuantity() + orderItem.getQuantity());
                        cart.setCartDate(LocalDateTime.now());
                        cartRepository.save(cart);



                    } else {
                        // 🔥 새로운 상품 추가
                        Cart newCart = new Cart(cstmNumber, orderItem.getProductId(),
                                orderItem.getQuantity(), karatCode);
                        cartRepository.save(newCart);

                    }
                }
            }

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    /**
     * 🔥 바로구매 결제 실패 처리 (장바구니는 그대로 유지)
     * 실제로는 아무것도 하지 않음 - 이미 백그라운드에서 장바구니에 추가되어 있으므로
     */
    public void handleDirectBuyPaymentFailure(String orderNumber, String cstmNumber) {
        try {

            // 장바구니는 이미 백그라운드에서 추가되어 있으므로 별도 처리 불필요

        } catch (Exception e) {

        }
    }

}