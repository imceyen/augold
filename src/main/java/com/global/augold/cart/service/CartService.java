package com.global.augold.cart.service;

import com.global.augold.cart.dto.CartDTO;
import com.global.augold.cart.entity.Cart;
import com.global.augold.cart.repository.CartRepository;
import com.global.augold.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.global.augold.product.entity.Product;


// ✅ 필요한 import만 정리
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
            // 1. 먼저 Cart의 product_id로 기준 상품 찾기
            Optional<Product> baseProduct = productRepository.findById(cartProductId);

            if (baseProduct.isPresent() && baseProduct.get().getProductGroup() != null) {
                // 2. 같은 그룹에서 해당 K값 상품 찾기
                List<Product> groupProducts = productRepository.findByProductGroup(baseProduct.get().getProductGroup());

                Optional<Product> correctProduct = groupProducts.stream()
                        .filter(p -> karatCode.equals(p.getKaratCode()))
                        .findFirst();

                if (correctProduct.isPresent()) {
                    return correctProduct.get().getFinalPrice(); // 🔥 올바른 K값 가격
                }
            }

            // 3. 못 찾으면 기존 방식 (기본값)
            return baseProduct.map(Product::getFinalPrice).orElse(0.0);

        } catch (Exception e) {
            return 0.0; // 오류 시 기본값
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
}