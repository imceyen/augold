package com.global.augold.cart.repository;

import com.global.augold.cart.dto.CartDTO;
import com.global.augold.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CartRepository extends JpaRepository<Cart, String> {

    // Cart는 Repository가 다룰 엔티티를 지정, String은 Cart 엔티티의 기본키 타입을 지정
    // cartNumber 필드가 String 타입의 기본키
    // ~로 찾아라 findBy
    List<Cart> findByCstmNumber(String cstmNumber);
    // 반환타입, 메소드 이름의 패턴, 매개변수 타입으로 이루어져있다.

    // 있는지 확인 existsBy
    boolean existsByCstmNumberAndProductId(String cstmNumber, String productId);
    // 이미 장바구니에 담긴 상품을 다시 담으려고 할 때 시스템이 적절하게 반응할 수 있게 되었습니다.

    // 조건부 삭제 deleteBy
    @Modifying
    @Transactional
    int deleteByCstmNumberAndProductId(String cstmNumber, String productId); // 원하는것 삭제

    @Modifying
    @Transactional
    int deleteByCstmNumber(String cstmNumber); // 전체 삭제

    int countByCstmNumber(String cstmNumber); // 개수 조회


    @Query("SELECT c FROM Cart c WHERE c.cstmNumber = :cstmNumber ORDER BY c.cartDate DESC")
    List<Cart> findByCstmNumberSimple(@Param("cstmNumber") String cstmNumber);

//    @Query("SELECT new com.global.augold.cart.dto.CartDTO(c.cartNumber, c.productId, c.cartDate, c.cstmNumber, p.productName, p.finalPrice, p.imageUrl, cat.ctgrNm, gk.karatName, p.goldWeight) " +
//            "FROM Cart c JOIN Product p ON c.productId = p.productId " +
//            "JOIN Category cat ON p.ctgrId = cat.ctgrId " +
//            "JOIN GoldKarat gk ON p.karatCode = gk.karatCode " +
//            "WHERE c.cstmNumber = :cstmNumber " +
//            "ORDER BY c.cartDate DESC")
//    List<CartDTO> findByCstmNumberWithProduct(@Param("cstmNumber") String cstmNumber);
}