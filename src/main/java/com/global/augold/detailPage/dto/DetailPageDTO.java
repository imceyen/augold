package com.global.augold.detailPage.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DetailPageDTO {
    private String productId;
    private String productName;
    private double finalPrice;
    private String imageUrl;
    private String description;
    private String imageUrl1;
    private String imageUrl2;
    private String imageUrl3;
}
