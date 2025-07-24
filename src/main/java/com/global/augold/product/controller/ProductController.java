package com.global.augold.product.controller;

import com.global.augold.product.dto.ProductDTO;
import com.global.augold.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 상품 관련 API를 처리하는 컨트롤러 (최종 수정 버전)
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ✅ 파일 업로드 경로를 상수로 관리하여 일관성을 유지합니다.
    private final String UPLOAD_DIR = "src/main/resources/static/upload/";

    /**
     * 전체 상품 목록 조회
     */
    @GetMapping
    public List<ProductDTO> getAllProducts() {
        return productService.getAllProducts();
    }

    /**
     * 상품 ID로 단일 조회
     */
    @GetMapping("/{id}")
    public ProductDTO getProductById(@PathVariable String id) {
        return productService.getProductById(id);
    }

    /**
     * ✅ 상품 등록 (파일 업로드와 폼 데이터 처리를 하나로 통합)
     * 이 메소드가 /api/products 경로의 POST 요청을 처리합니다.
     */
    @PostMapping
    public ProductDTO createProduct(
            // ✅ HTML form의 필드 이름과 DTO의 필드 이름이 일치하면 Spring이 자동으로 객체에 값을 채워줍니다.
            ProductDTO productDTO,

            // ✅ 대표 이미지는 필수로 받습니다.
            @RequestParam("imageFile") MultipartFile imageFile,

            // ✅ 상세 이미지는 선택사항(없어도 오류 없음)으로 받습니다.
            @RequestParam(value = "detailImages", required = false) List<MultipartFile> detailImages
    ) {
        try {
            // --- 1. 대표 이미지 저장 ---
            if (imageFile.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "대표 이미지는 필수입니다.");
            }
            File uploadPath = new File(UPLOAD_DIR);
            if (!uploadPath.exists()) {
                uploadPath.mkdirs();
            }
            String imageFileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
            Path imagePath = Paths.get(UPLOAD_DIR + imageFileName);
            Files.write(imagePath, imageFile.getBytes());
            productDTO.setImageUrl("/upload/" + imageFileName); // DTO에 이미지 경로 설정

            // --- 2. 상세 이미지 저장 ---
            if (detailImages != null && !detailImages.isEmpty()) {
                List<String> detailUrls = new ArrayList<>();
                for (MultipartFile file : detailImages) {
                    if (file.isEmpty()) continue; // 비어있는 파일 입력은 건너뜁니다.

                    String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                    Path path = Paths.get(UPLOAD_DIR + fileName);
                    Files.write(path, file.getBytes());
                    detailUrls.add("/upload/" + fileName);
                }

                // DTO에 상세 이미지 경로들을 설정합니다. (기존 로직 유지)
                if (detailUrls.size() > 0) productDTO.setImageUrl1(detailUrls.get(0));
                if (detailUrls.size() > 1) productDTO.setImageUrl2(detailUrls.get(1));
                if (detailUrls.size() > 2) productDTO.setImageUrl3(detailUrls.get(2));
            }

            // --- 3. 완성된 DTO를 서비스로 보내 DB에 저장 ---
            return productService.saveProduct(productDTO);

        } catch (IOException e) {
            e.printStackTrace();
            // 파일 처리 중 오류 발생 시 500 서버 에러를 반환합니다.
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 상품 삭제
     */
    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
    }

    /**
     * productId 자동생성
     */
    @GetMapping("/next-id")
    public String generateProductId(@RequestParam String subCtgr) {
        return productService.generateNextProductId(subCtgr);
    }
}