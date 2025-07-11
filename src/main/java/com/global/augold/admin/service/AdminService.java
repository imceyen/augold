package com.global.augold.admin.service;

import org.springframework.stereotype.Service;

/**
 * 관리자 관련 비즈니스 로직 처리 클래스
 */
@Service
public class AdminService {

    /**
     * 관리자 로그인 처리
     * 임시로 'admin' / '1234' 계정만 로그인 가능
     */
    public boolean login(String username, String password) {
        return "admin".equals(username) && "1234".equals(password);
    }
}
