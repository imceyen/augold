package com.global.augold.product.service;

import com.global.augold.product.dto.ProductDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    public List<ProductDTO> getAllProducts() {
        List<ProductDTO> products = new ArrayList<>();
        products.add(new ProductDTO(1L, "골드바 1g", 60000));
        products.add(new ProductDTO(2L, "골드 목걸이", 150000));
        products.add(new ProductDTO(3L, "선물용 골드링", 120000));
        return products;
    }
}
