package com.global.augold.product.repository;

import com.global.augold.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {

    // 카테고리별 상품 조회 (예: 골드바, 주얼리 등)
    List<Product> findByCategoryId(String categoryId);

    // 상품 이름으로 검색
    List<Product> findByProductNameContaining(String keyword);
}
