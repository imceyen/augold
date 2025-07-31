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
            @ModelAttribute Customer customer, // 사용자가 입력한 폼 데이터를 받습니다.
            @RequestParam String postcode,
            @RequestParam String address,
            @RequestParam String detailAddress,
            RedirectAttributes redirectAttributes) {

        // 주소 필드를 하나의 문자열로 합칩니다.
        String fullAddr = postcode + " " + address + " " + detailAddress;
        customer.setCstmAddr(fullAddr);

        try {
            // 서비스 레이어의 회원가입 로직을 호출합니다.
            customerService.register(customer);

            // 성공 시: 로그인 페이지로 리다이렉트하며 성공 메시지를 전달합니다.
            redirectAttributes.addFlashAttribute("message", "회원가입이 성공적으로 완료되었습니다. 로그인해주세요.");
            return "redirect:/login"; // 로그인 페이지 경로로 수정하는 것을 권장

        } catch (IllegalArgumentException e) {
            // 실패 시: (예: 아이디 중복) 다시 회원가입 폼으로 리다이렉트합니다.

            // 1. 서비스에서 발생한 에러 메시지를 Flash Attribute로 전달합니다.
            //    Flash Attribute는 리다이렉트 후 딱 한 번만 사용되고 사라집니다.
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());

            // 2. 사용자가 입력했던 데이터(customer 객체)도 함께 전달하여,
            //    폼에 기존 입력 내용이 사라지지 않도록 합니다.
            redirectAttributes.addFlashAttribute("customer", customer);

            return "redirect:/customer/register";
        }
    }
}