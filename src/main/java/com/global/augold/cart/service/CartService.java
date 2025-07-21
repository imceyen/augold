package com.global.augold.cart.service;

import com.global.augold.cart.dto.CartDTO;
import com.global.augold.cart.entity.Cart;
import com.global.augold.cart.repository.CartRepository;
import com.global.augold.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        // 재고 체크
        Integer inventory = productRepository.findById(productId)
                .map(product -> product.getProductInventory())
                .orElse(0);

        if (inventory <= 0) {
            throw new RuntimeException("재고가 부족하여 장바구니에 담을 수 없습니다.");
        }

        // ✅ 기존 아이템 찾기 (같은 상품 + 같은 순도)
        Optional<Cart> existingCart = cartRepository.findByCstmNumberAndProductIdAndKaratCode(cstmNumber, productId, karatCode);

        if (existingCart.isPresent()) {
            // ✅ 기존 아이템이 있으면 수량 증가
            Cart cart = existingCart.get();
            int newQuantity = cart.getQuantity() + quantity;

            // 재고 확인 (기존 수량 + 새로 담을 수량)
            if (newQuantity > inventory) {
                throw new RuntimeException("보유 재고를 초과하여 장바구니에 담을 수 없습니다.");
            }

            cart.setQuantity(newQuantity);
            cart.setCartDate(LocalDateTime.now()); // 날짜 업데이트

            try {
                cartRepository.save(cart);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("장바구니 수량 업데이트 중 오류가 발생했습니다: " + e.getMessage());
            }

        } else {
            // ✅ 기존 아이템이 없으면 새로 생성
            if (quantity > inventory) {
                throw new RuntimeException("보유 재고를 초과하여 장바구니에 담을 수 없습니다.");
            }

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
            // 복합 쿼리로 모든 정보를 한번에 가져오기
            List<Object[]> rawResults = cartRepository.findByCstmNumberWithProduct(cstmNumber);
            List<CartDTO> cartItems = new ArrayList<>();

            for (Object[] row : rawResults) {
                CartDTO dto = new CartDTO(
                        (String) row[0],           // cart_number
                        (String) row[1],           // product_id
                        ((java.sql.Timestamp) row[2]).toLocalDateTime(), // cart_date
                        (String) row[3],           // cstm_number
                        (String) row[4],           // product_name
                        row[5] != null ? ((Number) row[5]).doubleValue() : 0.0, // final_price
                        (String) row[6],           // image_url
                        (String) row[7],           // ctgr_id
                        (String) row[8],           // c.karat_code ← Cart의 karat_code
                        (String) row[9],           // product_group
                        row[10] != null ? ((Number) row[10]).intValue() : 1  // ✅ c.quantity 추가
                );
                cartItems.add(dto);
            }


            return cartItems;

        } catch (Exception e) {
            // 복합 쿼리 실패 시 기본 정보만 반환 (fallback)
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