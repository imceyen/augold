package com.global.augold.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // ✅ 모든 요청 인증 없이 허용
                )
                .formLogin(form -> form.disable()) // ✅ 로그인 폼 비활성화
                .csrf(csrf -> csrf.disable())      // ✅ CSRF 비활성화
                .build();                          // ✅ 마지막에 build()로 체이닝 마무리
    }
}
