package com.global.augold.member.controller;

import com.global.augold.member.entity.Customer;
import com.global.augold.member.service.CustomerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService){
        this.customerService = customerService;
    }

    /**
     * 회원가입 폼을 보여주는 메서드 (수정된 버전)
     * 1. 최초로 페이지에 접속할 때는 빈 회원 객체를 모델에 담아 폼을 보여줍니다.
     * 2. 회원가입 실패로 인해 리다이렉트될 경우, RedirectAttributes를 통해 전달된
     *    'errorMessage'와 사용자가 입력했던 데이터('customer')가 자동으로 모델에 추가되어
     *    오류 메시지와 함께 입력 내용이 그대로 표시됩니다.
     */
    @GetMapping("/register")
    public String registerForm(Model model) {
        // 모델에 "customer" 속성이 없다면 (최초 접속 시) 빈 객체를 추가합니다.
        // 이 처리를 통해, 리다이렉트로 전달된 사용자 입력값("customer")이 있을 경우 그것을 사용하게 됩니다.
        if (!model.containsAttribute("customer")) {
            model.addAttribute("customer", new Customer());
        }
        return "member/signup"; // templates/member/signup.html 렌더링
    }

    /**
     * 회원가입 요청을 처리하는 메서드 (수정된 버전)
     */
    @PostMapping("/register")
    public String register(
            @ModelAttribute Customer customer,
            @RequestParam(required = false) String postcode,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String detailAddress,
            RedirectAttributes redirectAttributes) {

        // 주소가 null인지 체크
        if (postcode == null || address == null || detailAddress == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "주소 정보를 모두 입력해주세요.");
            return "redirect:/customer/register";
        }

        String fullAddr = postcode + " " + address + " " + detailAddress;
        customer.setCstmAddr(fullAddr);
        System.out.println("Full Address: " + fullAddr);

        try {
            customerService.register(customer);
            redirectAttributes.addFlashAttribute("message", "회원가입이 성공적으로 완료되었습니다. 로그인해주세요.");
            return "redirect:/login";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("customer", customer);
            return "redirect:/customer/register";
        }
    }
}