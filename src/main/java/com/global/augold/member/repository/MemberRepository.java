//package com.global.augold.member.repository;
//
//import com.global.augold.member.entity.Member;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//// JpaRepository<엔티티, ID타입> 상속하면 DB 기본 CRUD 사용 가능!
//public interface MemberRepository extends JpaRepository<Member, Long> {
//
//    // 추가로 원하는 쿼리 작성 가능
//    Member findByUsername(String username); // 사용자 아이디로 조회
//}
