package com.global.augold.categoryPage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryPageDTO {
    private Long productId;
    private String productName;
    private int quantity;
    private int pricePerUnit;
}
