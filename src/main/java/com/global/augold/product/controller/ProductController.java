package com.global.augold.product.controller;

import com.global.augold.product.dto.ProductDTO;
import com.global.augold.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /**
     * productId 자동생성
     */
    @GetMapping("/next-id")
    public String generateProductId(@RequestParam String subCtgr) {
        return productService.generateNextProductId(subCtgr);
    }


}