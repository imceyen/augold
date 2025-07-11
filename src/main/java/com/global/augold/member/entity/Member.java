//package com.global.augold.member.entity;
//
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//
//@Entity // 이 클래스는 JPA가 관리하는 엔티티(=DB 테이블과 매핑됨)
//@Getter  // Lombok: getter 자동 생성
//@NoArgsConstructor // Lombok: 기본 생성자 자동 생성
//public class Member {
//
//    @Id // PK (기본키)
//    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB가 자동으로 증가시킴 (auto_increment)
//    private Long id;
//
//    @Column(nullable = false, unique = true) // not null + 중복 불가
//    private String username; // 사용자 아이디
//
//    @Column(nullable = false)
//    private String password; // 비밀번호
//
//    @Column(nullable = false, unique = true)
//    private String email; // 이메일
//
//    @Column(nullable = false)
//    private String role; // 권한: USER or ADMIN
//
//    // 생성자 (username, password, email, role만 받는 생성자)
//    public Member(String username, String password, String email, String role) {
//        this.username = username;
//        this.password = password;
//        this.email = email;
//        this.role = role;
//    }
//}
