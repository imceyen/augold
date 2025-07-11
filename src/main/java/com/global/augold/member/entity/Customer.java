package com.global.augold.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="CUSTOMER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @Column(name = "CSTM_NUMBER")
    private String cstmNumber;  // 회원번호

    @Column(name = "CSTM_ID", length = 20, nullable = false, unique = true)
    private String cstmId;

    @Column(name = "CSTM_PWD", nullable = false)
    private String cstmPwd;

    @Column(name = "CSTM_NAME", length = 30, nullable = false)
    private String cstmName;

    @Column(name = "CSTM_ADDR", nullable = false, columnDefinition = "TEXT")
    private String cstmAddr;

    @Column(name = "CSTM_PHONE", length = 15, nullable = false)
    private String cstmPhone;

    @Column(name = "CSTM_GENDER", length = 1, nullable = false)
    private String cstmGender;

    @Column(name = "CSTM_BIRTH", nullable = false)
    private LocalDate cstmBirth;

    @Column(name = "REG_DATE", nullable = false)
    private LocalDateTime regDate = LocalDateTime.now();
}