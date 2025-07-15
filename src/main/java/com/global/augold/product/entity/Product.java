package com.global.augold.product.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    @Id
    @Column(name = "product_id")
    private String productId;

    @Column(name = "karat_code")
    private String karatCode;

    @Column(name = "ctgr_id")
    private String ctgrId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "base_price")
    private Double basePrice;

    @Column(name = "gold_weight")
    private Double goldWeight;

    @Column(name = "final_price")
    private Double finalPrice;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "description")
    private String description;

    @Column(name = "sub_ctgr")
    private String subCtgr;

    private String productGroup;

    public String getSubCtgr() {
        return subCtgr;
    }
}