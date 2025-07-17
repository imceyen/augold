package com.global.augold.detailPage.repository;

import com.global.augold.detailPage.entity.ProductDetailImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface DetailPageRepository extends JpaRepository<ProductDetailImage, String> {

    List<ProductDetailImage> findByProductId(String productId);

    @Transactional
    void deleteByProductId(String productId);
}
