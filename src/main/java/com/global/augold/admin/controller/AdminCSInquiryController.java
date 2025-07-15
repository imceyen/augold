package com.global.augold.admin.controller;

import com.global.augold.admin.entity.AdminCSInquiry;
import com.global.augold.admin.service.AdminService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class AdminCSInquiryController {

    private final AdminService adminService;

    // 전체 문의 목록 조회 (검색어 optional)
    @GetMapping
    public List<AdminCSInquiry> getAllInquiries(@RequestParam(required = false) String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return adminService.getAllInquiries();
        }
        return adminService.searchInquiries(keyword.trim());
    }

    // 특정 문의 상세 조회 (JSON 반환)
    @GetMapping("/{inqNumber}")
    public ResponseEntity<AdminCSInquiry> getInquiry(@PathVariable String inqNumber) {
        return adminService.getInquiry(inqNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 답변 저장 및 수정
    @PostMapping("/{inqNumber}/answer")
    public ResponseEntity<?> saveAnswer(@PathVariable String inqNumber,
                                        @RequestBody AnswerRequest answerRequest) {
        try {
            adminService.saveAnswer(inqNumber, answerRequest.getAnswerContent());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 답변 삭제
    @DeleteMapping("/{inqNumber}/answer")
    public ResponseEntity<?> deleteAnswer(@PathVariable String inqNumber) {
        try {
            adminService.deleteAnswer(inqNumber);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Data
    public static class AnswerRequest {
        private String answerContent;
    }
}
