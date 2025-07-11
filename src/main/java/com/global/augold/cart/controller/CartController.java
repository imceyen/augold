
package com.global.augold.cart.controller;

import com.global.augold.cart.dto.CartDTO;
import com.global.augold.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping("")
    public String cartList(Model model) {
        String cstmNumber = "CSTM-00002"; // 세션으로 보낼꺼라 임시로 하드코딩
        List<CartDTO> cartItems = cartService.getCartList(cstmNumber);
        model.addAttribute("cartlist", cartItems);
        return "cart/cart";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam String productId) {
        String cstmNumber = "CSTM-00002"; // 세션으로 보낼꺼라 임시로 하드코딩
        String add = cartService.addToCart(cstmNumber, productId);
        return "redirect:/cart?added=true&productId=" + productId;
    }

    @PostMapping("/remove")
    public String deleteFromCart(@RequestParam List<String> productIds) {
        String cstmNumber = "CSTM-00002"; // 세션으로 받을꺼임
        boolean remove = cartService.deleteCartItems(cstmNumber, productIds);
        return "redirect:/cart?removed=true";
    }

    @PostMapping("/removeall")
    public String deleteAllFromCart() {
        String cstmNumber = "CSTM-00002"; // 세션으로 받을꺼임
        boolean removeAll = cartService.deleteAllItems(cstmNumber);
        return "redirect:/cart?removedAll=true";
    }

    @GetMapping("/count")
    @ResponseBody
    public int getCartCount() {
        String cstmNumber = "CSTM-00002";
        int count = cartService.getCartCount(cstmNumber);
        return count;
    }
}
