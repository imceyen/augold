package com.global.augold.admin.repository;

import com.global.augold.admin.entity.AdminCSInquiry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminCSInquiryRepository extends JpaRepository<AdminCSInquiry, String> {
    // 필요하면 검색용 쿼리 메소드 추가 가능
}
