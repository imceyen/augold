package com.global.augold.cart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity // 테이블과 연결되는 특별한 개체야
@Table(name = "cart") // 데이터베이스에서 실제 테이블 이름 지정
@Data
@NoArgsConstructor // 매개변수 없는 생성자 만들기
@AllArgsConstructor // 모든 필드를 매개 변수로 받는 생성자 만들기
public class Cart {
    @Id // 이 필드가 테이블의 기본키
    @Column(name = "CART_NUMBER") // 이 java 필드가 데이터베이스의 CART_NUMBER컬럼과 연결돼
    private String cartNumber; // 트리거에서 자동 생성되므로 @GenerateValue 사용 X

    @Column(name = "CSTM_NUMBER")
    private String cstmNumber; // 회원번호

    @Column(name = "Product_ID")
    private String productId; // 상품번호

    @Column(name = "CART_DATE")
    private  LocalDateTime cartDate; // 담은날짜

    // 비즈니스 로직용 생성자 = 장바구니에 상품을 담을 때 사용
    public  Cart(String cstmNumber, String productId){
        // 사용자 정의 생성자
        this.cartNumber = "";
        this.cstmNumber = cstmNumber;
        this.productId = productId;
        this.cartDate = LocalDateTime.now();
    }
}