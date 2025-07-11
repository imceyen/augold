package com.global.augold.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor   // 기본 생성자
@AllArgsConstructor  // 모든 필드를 파라미터로 받는 생성자
public class ProductDTO {
    private Long id;
    private String name;
    private int price;
}