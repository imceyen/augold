package com.global.augold.member.repository;

import com.global.augold.member.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {

    // 아이디가 있는지 체크
    boolean existsByCstmId(String cstmId);

    // 아이디 비번 둘다 체크
    Optional<Customer> findByCstmIdAndCstmPwd(String cstmId, String cstmPwd);
    // ✅ 로그인용: 아이디로 회원 조회
    Optional<Customer> findByCstmId(String cstmId);

    // ✅ PaymentService에서 사용하는 메서드 추가
    Optional<Customer> findByCstmNumber(String cstmNumber);

    // 번호로 찾기
    Optional<Customer> findByCstmPhone(String cstmPhone);
    boolean existsByCstmNumber(String cstmNumber);
}

