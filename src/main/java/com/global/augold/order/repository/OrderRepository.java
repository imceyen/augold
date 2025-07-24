// ===============================
// OrderRepository.java
// ===============================

package com.global.augold.order.repository;

import com.global.augold.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    // 주문번호로 조회 (단건)
    Optional<Order> findByOrderNumber(String orderNumber);

    // 고객번호로 주문 목록 조회 (최신순)
    List<Order> findByCstmNumberOrderByOrderDateDesc(String cstmNumber);

    // 주문 상태로 조회
    List<Order> findByOrderStatus(Order.OrderStatus orderStatus);

    // 주문 상태별 조회 (최신순)
    List<Order> findByOrderStatusOrderByOrderDateDesc(Order.OrderStatus orderStatus);

    // 고객번호와 주문 상태로 조회
    List<Order> findByCstmNumberAndOrderStatus(String cstmNumber, Order.OrderStatus orderStatus);

    List<Order> findByCstmNumberAndOrderStatusNotOrderByOrderDateDesc(
            String cstmNumber, Order.OrderStatus excludeStatus);

    // 특정 기간 내 주문 조회
    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate ORDER BY o.orderDate DESC")
    List<Order> findByOrderDateBetween(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    // 고객의 최근 주문 조회 (개수 제한)
    @Query("SELECT o FROM Order o WHERE o.cstmNumber = :cstmNumber ORDER BY o.orderDate DESC LIMIT :limit")
    List<Order> findRecentOrdersByCustomer(@Param("cstmNumber") String cstmNumber, @Param("limit") int limit);

    // 특정 금액 이상 주문 조회
    @Query("SELECT o FROM Order o WHERE o.finalAmount >= :amount ORDER BY o.finalAmount DESC")
    List<Order> findByFinalAmountGreaterThanEqual(@Param("amount") BigDecimal amount);

    // 고객별 총 주문 금액 계산
    @Query("SELECT SUM(o.finalAmount) FROM Order o WHERE o.cstmNumber = :cstmNumber AND o.orderStatus = 'PAID'")
    BigDecimal getTotalOrderAmountByCustomer(@Param("cstmNumber") String cstmNumber);

    // 고객별 주문 건수 계산
    @Query("SELECT COUNT(o) FROM Order o WHERE o.cstmNumber = :cstmNumber")
    Long getOrderCountByCustomer(@Param("cstmNumber") String cstmNumber);

    // 특정 기간 내 고객 주문 조회
    @Query("SELECT o FROM Order o WHERE o.cstmNumber = :cstmNumber " +
            "AND o.orderDate BETWEEN :startDate AND :endDate " +
            "ORDER BY o.orderDate DESC")
    List<Order> findByCustomerAndDateRange(@Param("cstmNumber") String cstmNumber,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    // 일별 주문 통계
    @Query("SELECT FUNCTION('DATE', o.orderDate) as orderDate, COUNT(o), SUM(o.finalAmount) " +
            "FROM Order o WHERE o.orderStatus = 'PAID' " +
            "GROUP BY FUNCTION('DATE', o.orderDate) " +
            "ORDER BY orderDate DESC")
    List<Object[]> getDailyOrderStats();

    // 월별 주문 통계
    @Query("SELECT FUNCTION('DATE_FORMAT', o.orderDate, '%Y-%m') as month, " +
            "COUNT(o), SUM(o.finalAmount) FROM Order o " +
            "WHERE o.orderStatus = 'PAID' " +
            "GROUP BY FUNCTION('DATE_FORMAT', o.orderDate, '%Y-%m') " +
            "ORDER BY month DESC")
    List<Object[]> getMonthlyOrderStats();

    // 배송지별 주문 조회 (부분 일치)
    @Query("SELECT o FROM Order o WHERE o.deliveryAddr LIKE %:address% ORDER BY o.orderDate DESC")
    List<Order> findByDeliveryAddressContaining(@Param("address") String address);

    // 특정 상태가 아닌 주문 조회
    List<Order> findByOrderStatusNotOrderByOrderDateDesc(Order.OrderStatus orderStatus);

    // 고객의 특정 상태 주문 중 최신 1건
    @Query("SELECT o FROM Order o WHERE o.cstmNumber = :cstmNumber AND o.orderStatus = :orderStatus " +
            "ORDER BY o.orderDate DESC LIMIT 1")
    Optional<Order> findLatestOrderByCustomerAndStatus(@Param("cstmNumber") String cstmNumber,
                                                       @Param("orderStatus") Order.OrderStatus orderStatus);

    // 주문 금액 범위로 조회
    @Query("SELECT o FROM Order o WHERE o.finalAmount BETWEEN :minAmount AND :maxAmount " +
            "ORDER BY o.finalAmount ASC")
    List<Order> findByAmountRange(@Param("minAmount") BigDecimal minAmount,
                                  @Param("maxAmount") BigDecimal maxAmount);

    // 오늘의 주문 조회
    @Query("SELECT o FROM Order o WHERE DATE(o.orderDate) = CURRENT_DATE ORDER BY o.orderDate DESC")
    List<Order> findTodayOrders();

    // 취소되지 않은 주문만 조회
    @Query("SELECT o FROM Order o WHERE o.orderStatus NOT IN ('CANCELLED', 'REFUNDED') " +
            "ORDER BY o.orderDate DESC")
    List<Order> findActiveOrders();
}