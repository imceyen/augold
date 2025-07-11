package com.global.augold.admin.entity;//package com.global.final2.admin.entity;
//
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//
//@Entity // JPA 엔티티 선언
//@Getter  // Lombok: getter 자동 생성
//@NoArgsConstructor // 기본 생성자 자동 생성
//public class Admin {
//
//    @Id // 기본키
//    @GeneratedValue(strategy = GenerationType.IDENTITY) // auto_increment
//    private Long id;
//
//    @Column(nullable = false, unique = true)
//    private String username; // 관리자 아이디
//
//    @Column(nullable = false)
//    private String password; // 관리자 비밀번호
//
//    @Column(nullable = false)
//    private String role; // 권한 (ex: "ADMIN")
//
//    // 생성자
//    public Admin(String username, String password, String role) {
//        this.username = username;
//        this.password = password;
//        this.role = role;
//    }
//}
