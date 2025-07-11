package com.global.augold.product.controller;

import com.global.augold.product.dto.ProductDTO;
import com.global.augold.product.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public List<ProductDTO> getProducts() {
        return productService.getAllProducts();
    }
}