package com.global.augold.product.controller;

import com.global.augold.detailPage.dto.DetailPageDTO;
import com.global.augold.detailPage.service.DetailPageService;
import com.global.augold.product.dto.ProductDTO;
import com.global.augold.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {


    private final ProductService productService;
    private final DetailPageService detailPageService;

    @GetMapping
    public List<ProductDTO> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public ProductDTO getProductById(@PathVariable String id) {
        return productService.getProductById(id);
    }

    @PostMapping
    public ProductDTO createProduct(@RequestBody ProductDTO productDTO) {
        return productService.saveProduct(productDTO);
    }

    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);


    }

    /**
     * 상품 등록 (파일 업로드 포함, 상세 이미지 3장 각각 따로 받음)
     */
    @PostMapping("/upload")
    public String uploadProduct(
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam(value = "detailImage1", required = false) MultipartFile detailImage1,
            @RequestParam(value = "detailImage2", required = false) MultipartFile detailImage2,
            @RequestParam(value = "detailImage3", required = false) MultipartFile detailImage3,
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

            // 상세 이미지 저장 및 URL 변수에 저장
            String detailUrl1 = null;
            String detailUrl2 = null;
            String detailUrl3 = null;

            if (detailImage1 != null && !detailImage1.isEmpty()) {
                String fileName = UUID.randomUUID() + "_" + detailImage1.getOriginalFilename();
                Path path = Paths.get(uploadDir + fileName);
                Files.write(path, detailImage1.getBytes());
                detailUrl1 = "/upload/" + fileName;
            }
            if (detailImage2 != null && !detailImage2.isEmpty()) {
                String fileName = UUID.randomUUID() + "_" + detailImage2.getOriginalFilename();
                Path path = Paths.get(uploadDir + fileName);
                Files.write(path, detailImage2.getBytes());
                detailUrl2 = "/upload/" + fileName;
            }
            if (detailImage3 != null && !detailImage3.isEmpty()) {
                String fileName = UUID.randomUUID() + "_" + detailImage3.getOriginalFilename();
                Path path = Paths.get(uploadDir + fileName);
                Files.write(path, detailImage3.getBytes());
                detailUrl3 = "/upload/" + fileName;
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

            // DTO에 상세 이미지 URL 넣기
            dto.setImageUrl1(detailUrl1);
            dto.setImageUrl2(detailUrl2);
            dto.setImageUrl3(detailUrl3);

            // 상품 저장
            ProductDTO savedProduct = productService.saveProduct(dto); // 저장 후 반환값에서 productId 받기

            // 상세 이미지 별도 테이블 저장
            DetailPageDTO detailDTO = DetailPageDTO.builder()
                    .productId(savedProduct.getProductId()) // 저장된 productId 사용
                    .imageUrl1(detailUrl1)
                    .imageUrl2(detailUrl2)
                    .imageUrl3(detailUrl3)
                    .build();

            detailPageService.saveDetailImages(detailDTO);

            return "등록 성공";

        } catch (IOException e) {
            e.printStackTrace();
            return "파일 저장 실패";
        }
    }

    @GetMapping("/next-id")
    public String generateProductId(@RequestParam String subCtgr) {
        return productService.generateNextProductId(subCtgr);
    }


}
