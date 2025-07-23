// ===============================
// PaymentRepository.java
// ===============================

package com.global.augold.payment.repository;

import com.global.augold.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    // 주문번호로 결제 정보 조회
    Optional<Payment> findByOrderNumber(String orderNumber);

    // 결제 상태로 조회
    List<Payment> findByPaymentStatus(Payment.PaymentStatus paymentStatus);

    // 결제 수단으로 조회
    List<Payment> findByPaymentMethod(Payment.PaymentMethod paymentMethod);

    // 날짜 범위로 결제 내역 조회
    @Query("SELECT p FROM Payment p WHERE p.paymentDate BETWEEN :startDate AND :endDate ORDER BY p.paymentDate DESC")
    List<Payment> findByPaymentDateBetween(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    // 고객별 결제 내역 조회 (Order와 조인)
    @Query("SELECT p FROM Payment p JOIN p.order o WHERE o.cstmNumber = :cstmNumber ORDER BY p.paymentDate DESC")
    List<Payment> findByCustomerNumber(@Param("cstmNumber") String cstmNumber);

    // 특정 금액 이상 결제 조회
    @Query("SELECT p FROM Payment p WHERE p.paymentAmount >= :amount ORDER BY p.paymentAmount DESC")
    List<Payment> findByPaymentAmountGreaterThanEqual(@Param("amount") java.math.BigDecimal amount);

    // 결제 완료된 건만 조회
    @Query("SELECT p FROM Payment p WHERE p.paymentStatus = 'COMPLETED' ORDER BY p.paymentDate DESC")
    List<Payment> findCompletedPayments();

    // 카드사별 결제 통계
    @Query("SELECT p.cardCompany, COUNT(p), SUM(p.paymentAmount) FROM Payment p " +
            "WHERE p.cardCompany IS NOT NULL AND p.paymentStatus = 'COMPLETED' " +
            "GROUP BY p.cardCompany")
    List<Object[]> getPaymentStatsByCardCompany();

    // 월별 결제 통계
    @Query("SELECT FUNCTION('DATE_FORMAT', p.paymentDate, '%Y-%m') as month, " +
            "COUNT(p), SUM(p.paymentAmount) FROM Payment p " +
            "WHERE p.paymentStatus = 'COMPLETED' " +
            "GROUP BY FUNCTION('DATE_FORMAT', p.paymentDate, '%Y-%m') " +
            "ORDER BY month DESC")
    List<Object[]> getMonthlyPaymentStats();

    // 결제 실패 건 조회
    List<Payment> findByPaymentStatusOrderByPaymentDateDesc(Payment.PaymentStatus paymentStatus);

    // 특정 기간 내 고객의 결제 내역
    @Query("SELECT p FROM Payment p JOIN p.order o " +
            "WHERE o.cstmNumber = :cstmNumber " +
            "AND p.paymentDate BETWEEN :startDate AND :endDate " +
            "ORDER BY p.paymentDate DESC")
    List<Payment> findByCustomerAndDateRange(@Param("cstmNumber") String cstmNumber,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);
}