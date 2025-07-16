package com.global.augold.member.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "CS_INQUIRY")
@Getter
@Setter
public class CSInquiry {

    @Id
    @Column(name = "INQ_NUMBER", length = 20)
    private String inqNumber;

    @Column(name = "CSTM_NUMBER", length = 20, nullable = false)
    private String cstmNumber;

    @Column(name = "INQ_CATEGORY", length = 20, nullable = false)
    private String inqCategory;

    @Column(name = "INQ_TITLE", length = 100, nullable = false)
    private String inqTitle;

    @Column(name = "INQ_CONTENT", nullable = false, columnDefinition = "TEXT")
    private String inqContent;

    @Column(name = "INQ_STATUS", length = 20, nullable = false)
    private String inqStatus;

    @Column(name = "INQ_DATE", nullable = false, updatable = false)
    private LocalDateTime inqDate;

    @Column(name = "REPLY_DATE")
    private LocalDateTime replyDate;

    @PrePersist
    public void prePersist() {
        if (inqDate == null) {
            inqDate = LocalDateTime.now();
        }
    }
}
