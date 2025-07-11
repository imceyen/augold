package com.global.augold.categoryPage.entity;//package com.global.final2.cart.entity;
//
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//
//@Entity // JPA가 관리하는 엔티티임을 선언
//@Getter
//@NoArgsConstructor
//public class Cart {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id; // 장바구니 ID (기본키)
//
//    @Column(nullable = false)
//    private Long memberId; // 회원 ID (추후 Member와 연관관계 설정 가능)
//
//    @Column(nullable = false)
//    private Long productId; // 상품 ID (추후 Product와 연관관계 설정 가능)
//
//    @Column(nullable = false)
//    private int quantity; // 수량
//
//    // 생성자
//    public Cart(Long memberId, Long productId, int quantity) {
//        this.memberId = memberId;
//        this.productId = productId;
//        this.quantity = quantity;
//    }
//}
