package com.global.augold.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "PRODUCT")
@Data
public class Product {

    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "KARAT_CODE", nullable = false)
    private String karatCode;

    @Column(name = "CTGR_ID", nullable = false)
    private String categoryId;

    @Column(name = "PRODUCT_NAME", nullable = false)
    private String productName;

    @Column(name = "BASE_PRICE", nullable = false)
    private double basePrice;

    @Column(name = "GOLD_WEIGHT", nullable = false)
    private double goldWeight;

    @Column(name = "FINAL_PRICE", nullable = false)
    private double finalPrice;

    @Column(name = "IMAGE_URL")
    private String imageUrl;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "SUB_CTGR")
    private String subCtgr;
}
