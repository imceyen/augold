package com.global.augold.categoryPage.service;

import com.global.augold.categoryPage.dto.CategoryPageDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CategoryPageService {

    public List<CategoryPageDTO> getCartItems() {
        List<CategoryPageDTO> cartItems = new ArrayList<>();
        cartItems.add(new CategoryPageDTO(1L, "골드바 1g", 2, 60000));
        cartItems.add(new CategoryPageDTO(2L, "골드 목걸이", 1, 150000));
        return cartItems;
    }
}
