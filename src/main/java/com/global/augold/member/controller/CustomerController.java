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

    // 회원가입 폼 보여주기
    @GetMapping("/register")
    public String registerForm(Model model,
                               @ModelAttribute("message") String message,
                               @ModelAttribute("errorMessage") String errorMessage) {
        model.addAttribute("customer", new Customer());
        return "member/signup";
    }

    // 회원가입 처리
    @PostMapping("/register")
    public String register(
            @ModelAttribute Customer customer,
            @RequestParam String postcode,
            @RequestParam String address,
            @RequestParam String detailAddress,
            RedirectAttributes redirectAttributes) {

        // 주소 합치기
        String fullAddr = postcode + " " + address + " " + detailAddress;
        customer.setCstmAddr(fullAddr);

        try {
            customerService.register(customer);
            redirectAttributes.addFlashAttribute("message", "회원가입이 완료되었습니다!");
            return "redirect:/";
        } catch (IllegalArgumentException e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.trim().isEmpty()) {
                errorMsg = "오류가 발생했습니다. 다시 시도해주세요.";
            }
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            return "redirect:/customer/register";
        }
    }
}
