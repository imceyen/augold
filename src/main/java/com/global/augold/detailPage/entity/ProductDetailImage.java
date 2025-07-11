package com.global.augold.detailPage.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "PRODUCT_DETAIL_IMAGE")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DETAIL_ID") // 실제 DB 컬럼명
    private Long id;

    @Column(name = "PRODUCT_ID") // 여기가 중요!
    private String productId;

    @Column(name = "IMAGE_URL")
    private String imageUrl;
}


