package com.global.augold.detailPage.entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;


@Entity
@Table(name = "PRODUCT_DETAIL_IMAGE")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailImage {

    @Id
    @Column(name = "DETAIL_ID")
    private String id;  // ✅ Long → String

    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "IMAGE_URL")
    private String imageUrl;
}



