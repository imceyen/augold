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
    private Double finalPrice;
    private String imageUrl;
    private String description;
    private String imageUrl1;
    private String imageUrl2;
    private String imageUrl3;
    private String productGroup;
    private String karatCode;
    private Double goldWeight;
    private String subCtgr;
    private String categoryId;
}
