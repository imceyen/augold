package com.global.augold.admin.service;

import com.global.augold.admin.entity.AdminCSInquiry;
import com.global.augold.admin.repository.AdminCSInquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminCSInquiryRepository inquiryRepository;

    public List<AdminCSInquiry> getAllInquiries() {
        return inquiryRepository.findAll();
    }

    public Optional<AdminCSInquiry> getInquiry(String inqNumber) {
        return inquiryRepository.findById(inqNumber);
    }

    public AdminCSInquiry saveAnswer(String inqNumber, String answerContent) {
        AdminCSInquiry inquiry = inquiryRepository.findById(inqNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문의번호입니다: " + inqNumber));

        inquiry.setAnswerContent(answerContent);
        inquiry.setReplyDate(LocalDateTime.now());
        inquiry.setInqStatus(answerContent == null || answerContent.trim().isEmpty() ? "처리중" : "답변완료");

        return inquiryRepository.save(inquiry);
    }

    public void deleteAnswer(String inqNumber) {
        AdminCSInquiry inquiry = inquiryRepository.findById(inqNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문의번호입니다: " + inqNumber));

        inquiry.setAnswerContent(null);
        inquiry.setReplyDate(null);
        inquiry.setInqStatus("처리중");

        inquiryRepository.save(inquiry);
    }

    public List<AdminCSInquiry> searchInquiries(String keyword) {
        String lowerKeyword = keyword.toLowerCase();

        // DB 직접 검색하는 게 아니라 모든 목록 불러와서 필터링하는 단순 예시
        // 데이터 많으면 Query Method 또는 @Query로 최적화 필요
        return inquiryRepository.findAll().stream()
                .filter(inq ->
                        inq.getCstmNumber().toLowerCase().contains(lowerKeyword) ||
                                inq.getInqTitle().toLowerCase().contains(lowerKeyword) ||
                                inq.getInqStatus().toLowerCase().contains(lowerKeyword)
                )
                .collect(Collectors.toList());
    }
}
