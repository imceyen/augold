package com.global.augold.cart.repository;

import com.global.augold.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.List;

@Repository
public interface CartRepository extends JpaRepository<Cart, String> {

    // Cart는 Repository가 다룰 엔티티를 지정, String은 Cart 엔티티의 기본키 타입을 지정
    // cartNumber 필드가 String 타입의 기본키
    // ~로 찾아라 findBy
    List<Cart> findByCstmNumber(String cstmNumber);
    // 반환타입, 메소드 이름의 패턴, 매개변수 타입으로 이루어져있다.



    // 이미 장바구니에 담긴 상품을 다시 담으려고 할 때 시스템이 적절하게 반응할 수 있게 되었습니다.

    // 조건부 삭제 deleteBy
    @Modifying
    @Transactional
    int deleteByCstmNumberAndProductId(String cstmNumber, String productId); // 원하는것 삭제

    @Modifying
    @Transactional
    int deleteByCstmNumber(String cstmNumber); // 전체 삭제

    @Modifying
    @Transactional
    int deleteByCstmNumberAndProductIdAndKaratCode(String cstmNumber, String productId, String karatCode);

    int countByCstmNumber(String cstmNumber); // 개수 조회

    List<Cart> findByCstmNumberOrderByCartDateDesc(String cstmNumber);

    Optional<Cart> findByCstmNumberAndProductIdAndKaratCode(String cstmNumber, String productId, String karatCode);

    @Query("SELECT c FROM Cart c WHERE c.cstmNumber = :cstmNumber ORDER BY c.cartDate DESC")

    List<Cart> findByCstmNumberSimple(@Param("cstmNumber") String cstmNumber);

    // 기존 쿼리에서 p.karat_code를 c.karat_code로 변경하고 c.quantity 추가 안그러면 product의 기본값이 카트에 담김
    @Query(value = "SELECT c.cart_number, c.product_id, c.cart_date, c.cstm_number, " +
            "p.product_name, p.final_price, p.image_url, p.ctgr_id, c.karat_code, p.product_group, c.quantity " +
            "FROM cart c JOIN product p ON c.product_id = p.product_id " +
            "WHERE c.cstm_number = :cstmNumber " +
            "ORDER BY c.cart_date DESC",
            nativeQuery = true)

    List<Object[]> findByCstmNumberWithProduct(@Param("cstmNumber") String cstmNumber);

    @Query("SELECT COUNT(c) FROM Cart c WHERE c.cstmNumber = :cstmNumber AND c.productId = :productId")
    // 카트 엔티티의 개수를 세어 반환, 고객번호가 매개변수와 일치하는 조건, : 는 외부 값 들어가는 곳(해당하는 고객 번호를 여기다 넣고, 그거랑 같은 게 조건")
    int countByCstmNumberAndProductId(@Param("cstmNumber") String cstmNumber, @Param("productId") String productId);
    // 특정 고객이 특정 상품을 몇 번 담았는지 -> 장바구니에 추가 하기 전 이미 해당 상품이 담겼는지 확인하는 요옫
}