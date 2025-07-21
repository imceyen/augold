
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
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.Map;

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
    @ResponseBody
    public ResponseEntity<?> addToCart(@RequestParam String productId,
                                       @RequestParam(defaultValue = "1") int quantity,
                                       @RequestParam(required = false) String karatCode,
                                       @RequestParam double finalPrice,
                                       HttpSession session) {
        try {
            String cstmNumber = getCstmNumberFromSession(session);

            if (cstmNumber == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "login_required"));
            }

            String result = cartService.addToCart(cstmNumber, productId, quantity, karatCode, finalPrice);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "장바구니에 추가되었습니다.",
                    "productId", productId,
                    "quantity", quantity,
                    "karatCode", karatCode != null ? karatCode : "",
                    "finalPrice", finalPrice
            ));

        } catch (RuntimeException e) {
            if(e.getMessage().contains("재고")){
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "out_of_stock", "message", "재고가 부족합니다."));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "login_required", "message", "로그인이 필요합니다."));
        }
    }

    // 현제 페이지 URL 생성 메서드
    private String getCurrentPageUrl(HttpServletRequest request, String productId){
        return "/product/" + productId;
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
    public ResponseEntity<String> decreaseQuantity(@RequestParam String productId,String karatCode,HttpSession session) {
        try {
            String cstmNumber = getCstmNumberFromSession(session);
            boolean result = cartService.decreaseQuantity(cstmNumber, productId, karatCode);
            return ResponseEntity.ok(result ? "success" : "failure");
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body("login_required");
        }
    }
}