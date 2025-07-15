package com.global.augold.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CS_INQUIRY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminCSInquiry {

    @Id
    @Column(name = "INQ_NUMBER", length = 20)
    private String inqNumber;

    @Column(name = "CSTM_NUMBER", length = 20, nullable = false)
    private String cstmNumber;

    @Column(name = "INQ_CATEGORY", length = 20, nullable = false)
    private String inqCategory;

    @Column(name = "INQ_TITLE", length = 100, nullable = false)
    private String inqTitle;

    @Column(name = "INQ_CONTENT", columnDefinition = "TEXT", nullable = false)
    private String inqContent;

    @Column(name = "INQ_STATUS", length = 20, nullable = false)
    private String inqStatus;

    @Builder.Default
    @Column(name = "INQ_DATE", nullable = false, updatable = false)
    private LocalDateTime inqDate = LocalDateTime.now();

    @Column(name = "REPLY_DATE")
    private LocalDateTime replyDate;

    @Column(name = "ANSWER_CONTENT", columnDefinition = "TEXT")
    private String answerContent;
}

