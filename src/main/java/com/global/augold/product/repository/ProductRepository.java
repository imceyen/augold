package com.global.augold.product.repository;

import com.global.augold.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 상품 정보를 위한 JPA 리포지토리 인터페이스
 */
public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findByProductIdStartingWith(String prefix);

}