package com.global.augold.admin.dto;

/**
 * 관리자 로그인 요청 시 사용되는 DTO
 */
public class AdminLoginRequestDTO {
    private String username;
    private String password;

    // 기본 생성자 (필수)
    public AdminLoginRequestDTO() {}

    // getter, setter
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}