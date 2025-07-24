// ===============================
// OrderItemRepository.java
// ===============================

package com.global.augold.order.repository;

import com.global.augold.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, String> {

    // 주문번호로 주문 상품 목록 조회
    List<OrderItem> findByOrderNumber(String orderNumber);

    // 상품번호로 주문 내역 조회
    List<OrderItem> findByProductId(String productId);

    // 주문번호와 상품번호로 조회 (특정 주문의 특정 상품)
    Optional<OrderItem> findByOrderNumberAndProductId(String orderNumber, String productId);

    // 주문별 총 상품 개수 조회
    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.orderNumber = :orderNumber")
    Integer getTotalQuantityByOrderNumber(@Param("orderNumber") String orderNumber);

    // 주문별 총 상품 금액 조회
    @Query("SELECT SUM(oi.finalAmount) FROM OrderItem oi WHERE oi.orderNumber = :orderNumber")
    BigDecimal getTotalAmountByOrderNumber(@Param("orderNumber") String orderNumber);

    // 상품별 총 판매량 조회
    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.productId = :productId")
    Integer getTotalSoldQuantityByProductId(@Param("productId") String productId);

    // 상품별 총 매출액 조회
    @Query("SELECT SUM(oi.finalAmount) FROM OrderItem oi WHERE oi.productId = :productId")
    BigDecimal getTotalSalesAmountByProductId(@Param("productId") String productId);

    // 특정 수량 이상 주문된 상품들 조회
    @Query("SELECT oi FROM OrderItem oi WHERE oi.quantity >= :quantity ORDER BY oi.quantity DESC")
    List<OrderItem> findByQuantityGreaterThanEqual(@Param("quantity") Integer quantity);

    // 특정 금액 이상 주문 상품들 조회
    @Query("SELECT oi FROM OrderItem oi WHERE oi.finalAmount >= :amount ORDER BY oi.finalAmount DESC")
    List<OrderItem> findByFinalAmountGreaterThanEqual(@Param("amount") BigDecimal amount);

    // 고객별 주문 상품 내역 조회 (Order와 조인)
    @Query("SELECT oi FROM OrderItem oi JOIN oi.order o WHERE o.cstmNumber = :cstmNumber ORDER BY o.orderDate DESC")
    List<OrderItem> findByCustomerNumber(@Param("cstmNumber") String cstmNumber);

    // 인기 상품 조회 (판매량 기준 상위 N개)
    @Query("SELECT oi.productId, SUM(oi.quantity) as totalQuantity FROM OrderItem oi " +
            "GROUP BY oi.productId ORDER BY totalQuantity DESC LIMIT :limit")
    List<Object[]> findTopSellingProducts(@Param("limit") int limit);

    // 매출 상위 상품 조회 (매출액 기준 상위 N개)
    @Query("SELECT oi.productId, SUM(oi.finalAmount) as totalSales FROM OrderItem oi " +
            "GROUP BY oi.productId ORDER BY totalSales DESC LIMIT :limit")
    List<Object[]> findTopRevenueProducts(@Param("limit") int limit);

    // 특정 기간 내 상품별 판매 통계
    @Query("SELECT oi.productId, SUM(oi.quantity), SUM(oi.finalAmount) FROM OrderItem oi " +
            "JOIN oi.order o WHERE o.orderDate BETWEEN :startDate AND :endDate " +
            "GROUP BY oi.productId ORDER BY SUM(oi.finalAmount) DESC")
    List<Object[]> getProductSalesStatsByPeriod(@Param("startDate") java.time.LocalDateTime startDate,
                                                @Param("endDate") java.time.LocalDateTime endDate);

    // 주문에 포함된 상품 종류 개수
    @Query("SELECT COUNT(DISTINCT oi.productId) FROM OrderItem oi WHERE oi.orderNumber = :orderNumber")
    Long getDistinctProductCountByOrderNumber(@Param("orderNumber") String orderNumber);

    // 단가별 주문 상품 조회
    @Query("SELECT oi FROM OrderItem oi WHERE oi.unitPrice BETWEEN :minPrice AND :maxPrice " +
            "ORDER BY oi.unitPrice ASC")
    List<OrderItem> findByUnitPriceRange(@Param("minPrice") BigDecimal minPrice,
                                         @Param("maxPrice") BigDecimal maxPrice);

    // 특정 고객이 주문한 특정 상품의 총 수량
    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi JOIN oi.order o " +
            "WHERE o.cstmNumber = :cstmNumber AND oi.productId = :productId")
    Integer getTotalQuantityByCustomerAndProduct(@Param("cstmNumber") String cstmNumber,
                                                 @Param("productId") String productId);

    // 특정 고객이 구매한 상품 목록 (중복 제거)
    @Query("SELECT DISTINCT oi.productId FROM OrderItem oi JOIN oi.order o " +
            "WHERE o.cstmNumber = :cstmNumber")
    List<String> findDistinctProductIdsByCustomer(@Param("cstmNumber") String cstmNumber);

    // 평균 주문 수량 계산
    @Query("SELECT AVG(oi.quantity) FROM OrderItem oi")
    Double getAverageOrderQuantity();

    // 평균 상품 단가 계산
    @Query("SELECT AVG(oi.unitPrice) FROM OrderItem oi")
    BigDecimal getAverageUnitPrice();

    // 상품별 평균 주문 수량
    @Query("SELECT oi.productId, AVG(oi.quantity) FROM OrderItem oi " +
            "GROUP BY oi.productId ORDER BY AVG(oi.quantity) DESC")
    List<Object[]> getAverageQuantityByProduct();

    // 주문 상품 개수별 통계 (몇 개씩 주문되는지)
    @Query("SELECT oi.quantity, COUNT(oi) FROM OrderItem oi " +
            "GROUP BY oi.quantity ORDER BY oi.quantity ASC")
    List<Object[]> getQuantityDistribution();
}