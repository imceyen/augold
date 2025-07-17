package com.global.augold.product.controller;

import com.global.augold.product.dto.ProductDTO;
import com.global.augold.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 상품 관련 API를 처리하는 컨트롤러
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

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
     * 상품 등록
     */
    @PostMapping
    public ProductDTO createProduct(@RequestBody ProductDTO productDTO) {
        return productService.saveProduct(productDTO);
    }

    /**
     * 상품 삭제
     */
    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
    }

    @PostMapping("/upload")
    public String uploadProduct(
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam("detailImages") List<MultipartFile> detailImages,
            @RequestParam Map<String, String> formData
    ) {
        try {
            String uploadDir = "src/main/resources/static/upload/";

            File uploadPath = new File(uploadDir);
            if (!uploadPath.exists()) uploadPath.mkdirs();

            // 대표 이미지 저장
            String imageFileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
            Path imagePath = Paths.get(uploadDir + imageFileName);
            Files.write(imagePath, imageFile.getBytes());

            // 상세 이미지 최대 3장 저장
            List<String> detailUrls = new ArrayList<>();
            for (int i = 0; i < Math.min(3, detailImages.size()); i++) {
                MultipartFile file = detailImages.get(i);
                String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                Path path = Paths.get(uploadDir + fileName);
                Files.write(path, file.getBytes());
                detailUrls.add("/upload/" + fileName);
            }

            // DTO 구성
            ProductDTO dto = new ProductDTO();
            dto.setProductId(formData.get("productId"));
            dto.setProductName(formData.get("productName"));
            dto.setCtgrId(formData.get("ctgrId"));
            dto.setSubCtgr(formData.get("subCtgr"));
            dto.setKaratCode(formData.get("karatCode"));
            dto.setGoldWeight(Double.parseDouble(formData.get("goldWeight")));
            dto.setBasePrice(Double.parseDouble(formData.get("basePrice")));
            dto.setFinalPrice(Double.parseDouble(formData.get("finalPrice")));
            dto.setDescription(formData.get("description"));
            dto.setImageUrl("/upload/" + imageFileName);
            if (detailUrls.size() > 0) dto.setImageUrl1(detailUrls.get(0));
            if (detailUrls.size() > 1) dto.setImageUrl2(detailUrls.get(1));
            if (detailUrls.size() > 2) dto.setImageUrl3(detailUrls.get(2));

            productService.saveProduct(dto);
            return "등록 성공";

        } catch (IOException e) {
            e.printStackTrace();
            return "파일 저장 실패";
        }
    }

    /**
     * productId 자동생성
     */
    @GetMapping("/next-id")
    public String generateProductId(@RequestParam String subCtgr) {
        return productService.generateNextProductId(subCtgr);
    }




}