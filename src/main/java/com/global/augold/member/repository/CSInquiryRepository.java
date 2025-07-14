package com.global.augold.member.repository;

import com.global.augold.member.entity.CSInquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CSInquiryRepository extends JpaRepository<CSInquiry, String> {

    @Query("SELECT MAX(c.inqNumber) FROM CSInquiry c WHERE c.inqNumber LIKE :todayPrefix")
    String findMaxInquiryNumberForToday(@Param("todayPrefix") String todayPrefix);

}
