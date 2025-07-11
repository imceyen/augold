    package com.global.augold.cart.service;

    import com.global.augold.cart.dto.CartDTO;
    import com.global.augold.cart.entity.Cart;
    import com.global.augold.cart.repository.CartRepository;
    import lombok.RequiredArgsConstructor;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import java.util.List;

    @Service
    @Transactional
    @RequiredArgsConstructor // final의 생성자를 자동으로 만들어줌
    public class CartService{
        private final CartRepository cartRepository; // final인 이유는 DB랑 상호작용하는 중요한 구성요소라 바뀌면 안되서.
        // CartRepository cartRepository  => 타입 + 변수명

        public String addToCart(String cstmNumber, String productId){

            // 장바구니에 먼저 있는지 확인할 변수
            boolean exists = cartRepository.existsByCstmNumberAndProductId(cstmNumber, productId);

            // Cart 생성과 저장
            Cart cart = new Cart(cstmNumber, productId);
            cartRepository.save(cart);

            if (exists) {
                return "장바구니에 수량이 추가되었습니다.";
            }
            return "장바구니에 담겼습니다.";
        }
        public List<CartDTO> getCartList(String cstmNumber){
            // 임시로 findByCstmNumberSimple 사용
            List<Cart> carts = cartRepository.findByCstmNumberSimple(cstmNumber);
            return carts.stream()
                    .map(cart -> new CartDTO(
                            cart.getCartNumber(),
                            cart.getProductId(),
                            cart.getCartDate(),
                            cart.getCstmNumber()
                    ))
                    .toList();
        }

//        public List<CartDTO> getCartList(String cstmNumber){
//            // 만약 복합 쿼리가 오류때문에 반환 못할시 기본 정보라도 반환하게 예외처리
//            try {
//                return cartRepository.findByCstmNumberWithProduct(cstmNumber);
//            } catch (Exception e) { // 기본정보는 CartDTO 생성자 순서에 맞게 배치
//                List<Cart> carts = cartRepository.findByCstmNumber(cstmNumber); // DB에서 해당 고객의 Cart엔티티들을 List로 가져와서 carts 변수에 저장
//                return carts.stream() // 이 리스트를 스트림으로 변환해서 하나씩 처리할 수 있게 하기.
//                        .map(cart -> new CartDTO( // cart를 CartDTO로 변환 Cart라서 형변환 필수 -> 는 이렇게 바꿔라 는 의미
//                            cart.getCartNumber(),
//                            cart.getProductId(),
//                            cart.getCartDate(),
//                            cart.getCstmNumber()
//                        )) // 없는건 null로 반환
//                        .toList(); // 변환된 CartDTO 객체들을 다시 List로 모아서 반환
//                // 전체 흐름 [cart1, cart2, cart3] -> 스트림 -> [cartDTO1, cartDTO2, cartDTO3] 원본 리스트를 스트림으로 하나씩 변환해서 cartDTO로 하나씩 변환함.
//            }
//        }
        // 카트안의 개수 조회 메서드
        public int getCartCount(String cstmNumber){ // 조회만 하면 되니깐 Repository에 있는 메서드 호출하기.
            return cartRepository.countByCstmNumber(cstmNumber);
        }

        public boolean deleteCartItems(String cstmNumber, List<String> productIds){
            try { // 예외 처리 시작
                int totalDelete = 0; // 삭제할 상품 갯수 초기화
                for (String productId : productIds){ // 삭제할 상품 목록(productIds)을 하나씩 꺼내서 반복 처리
                    int deleted = cartRepository.deleteByCstmNumberAndProductId(cstmNumber, productId); // 현재 상품을 장바구니에 삭제 시도
                    // deleted : 실제로 삭제된 개수
                    totalDelete += deleted;
                }

                return totalDelete == productIds.size();// 삭제된 개수가 요청한 개수와 같아야 성공
            } catch (Exception e) {
                return false;
            }

        }

        public boolean deleteAllItems(String cstmNumber){ // 장바구니에 있는 아이템들을 일괄로 삭제하는 메서드
            try {
                int deletedCount = cartRepository.deleteByCstmNumber(cstmNumber);
                return deletedCount > 0;
            } catch (Exception e) {
                return false;
            }
        }
    }

