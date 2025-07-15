package com.global.augold.product.repository;

import com.global.augold.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 상품 정보를 위한 JPA 리포지토리 인터페이스
 */
public interface ProductRepository extends JpaRepository<Product, String> {

    // 카테고리별 상품 조회 (예: 골드바, 주얼리 등)
    List<Product> findByCategoryId(String categoryId);

    List<Product> findByProductIdStartingWith(String prefix);

    // 상품 이름으로 검색
    List<Product> findByProductNameContaining(String keyword);
    List<Product> findByProductGroup(String productGroup);

}