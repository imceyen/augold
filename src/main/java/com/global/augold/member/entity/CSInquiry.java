package com.global.augold.member.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CS_INQUIRY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CSInquiry {

    @Id
    @Column(name = "INQ_NUMBER", length = 20)
    private String inqNumber; // 문의번호 (INQ-00001)

    @Column(name = "CSTM_NUMBER", length = 20, nullable = false)
    private String cstmNumber; // 회원번호

    @Column(name = "INQ_CATEGORY", length = 20, nullable = false)
    private String inqCategory; // 문의유형

    @Column(name = "INQ_TITLE", length = 100, nullable = false)
    private String inqTitle; // 문의제목

    @Column(name = "INQ_CONTENT", columnDefinition = "TEXT", nullable = false)
    private String inqContent; // 문의내용

    @Column(name = "INQ_STATUS", length = 20, nullable = false)
    private String inqStatus; // 처리상태

    @Column(name = "INQ_DATE", nullable = false, updatable = false)
    private LocalDateTime inqDate = LocalDateTime.now(); // 문의일시

    @Column(name = "REPLY_DATE")
    private LocalDateTime replyDate; // 답변일시
}
