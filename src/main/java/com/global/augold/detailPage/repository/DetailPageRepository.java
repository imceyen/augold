package com.global.augold.detailPage.repository;

import com.global.augold.detailPage.entity.ProductDetailImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetailPageRepository extends JpaRepository<ProductDetailImage, Long> {
    List<ProductDetailImage> findByProductId(String productId);
    void deleteByProductId(String productId);

}
