//package com.global.augold.member.service;
//
//import com.global.augold.member.entity.Customer;
//import com.global.augold.member.repository.CustomerRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.Optional;
//
//@Service
//public class LoginService {
//
//    @Autowired
//    private CustomerRepository customerRepository;
//
//    public Optional<Customer> login(LoginRequestDTO request) {
//        return customerRepository.findByCstmIdAndCstmPwd(
//                request.getCustomerId(), request.getPassword()
//        );
//    }
//}
//
