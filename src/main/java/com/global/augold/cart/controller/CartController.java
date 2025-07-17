
package com.global.augold.cart.controller;

import com.global.augold.cart.dto.CartDTO;
import com.global.augold.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.global.augold.member.entity.Customer;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @ModelAttribute
    public void addLoginName(Model model, HttpSession session){ // 회원 이름 가져오는 메서드
        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if (loginUser != null){
           model.addAttribute("loginName", loginUser.getCstmName());
        }
    }

    private String getCstmNumberFromSession(HttpSession session) { // 고객 정보 세션에서 가져오는 메서드
        Object loginUserObj = session.getAttribute("loginUser");

        Customer loginUser = (Customer) session.getAttribute("loginUser");
        if(loginUser == null){
            throw new RuntimeException("로그인 후 사용할 수 있습니다.");
        }
        return loginUser.getCstmNumber();
    }



    @GetMapping("")
    public String cartList(Model model, HttpSession session) {
        // ✅ 세션 디버깅 추가


        Object loginUserObj = session.getAttribute("loginUser");


        try {
            String cstmNumber = getCstmNumberFromSession(session);


            List<CartDTO> cartItems = cartService.getCartList(cstmNumber);


            model.addAttribute("cartlist", cartItems);
            return "cart/cart";
        } catch (RuntimeException e) {

            e.printStackTrace();

            // 로그인하지 않은 경우 로그인 페이지로 리다이렉트
            return "redirect:/login?error=login&returnUrl=/cart";
        }
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam String productId, HttpSession session, HttpServletRequest request) {
        try {
            String cstmNumber = getCstmNumberFromSession(session);
            String add = cartService.addToCart(cstmNumber, productId);
            return "redirect:/product/" + productId + "?success=cart_added";
        } catch (RuntimeException e) { // 재고 부족이나 기타 장바구니 오류
            if(e.getMessage().contains("재고")){
                return "redirect:/product/detail/" + productId + "?error=out_of_stock";
            }

            // 로그인 관련 오류 시 현재 상품 페이지 URL을 returnURL로 전달함.
            String returnUrl = getCurrentPageUrl(request, productId);
            return "redirect:/login?error=login&returnUrl=" + URLEncoder.encode(returnUrl, StandardCharsets.UTF_8);
        }
    }

    // 현제 페이지 URL 생성 메서드
    private String getCurrentPageUrl(HttpServletRequest request, String productId){
        return "/product/detail/" + productId;
    }

    @PostMapping("/remove")
    public String deleteFromCart(@RequestParam List<String> productIds, HttpSession session) {
        try {
            String cstmNumber = getCstmNumberFromSession(session);
            boolean remove = cartService.deleteCartItems(cstmNumber, productIds);
            return "redirect:/cart?removed=true";
        } catch (RuntimeException e) {
            return "redirect:/login?error=login";
        }
    }

    @PostMapping("/removeall")
    public String deleteAllFromCart(HttpSession session) {
        try {
            String cstmNumber = getCstmNumberFromSession(session);
            boolean removeAll = cartService.deleteAllItems(cstmNumber);
            return "redirect:/cart?removedAll=true";
        } catch (RuntimeException e) {
            return "redirect:/login?error=login";
        }
    }

    @GetMapping("/count")
    @ResponseBody
    public int getCartCount(HttpSession session) {
        try {
            String cstmNumber = getCstmNumberFromSession(session);
            return cartService.getCartCount(cstmNumber);
        } catch (RuntimeException e) {
            return 0; // 로그인하지 않은 경우 0 반환
        }
    }

    @PostMapping("/decrease")
    @ResponseBody
    public ResponseEntity<String> decreaseQuantity(@RequestParam String productId, HttpSession session) {
        try {
            String cstmNumber = getCstmNumberFromSession(session);
            boolean result = cartService.decreaseQuantity(cstmNumber, productId);
            return ResponseEntity.ok(result ? "success" : "failure");
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body("login_required");
        }
    }
}