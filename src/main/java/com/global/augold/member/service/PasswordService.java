//package com.global.augold.member.service;
//
//import com.global.augold.member.entity.Customer;
//import com.global.augold.member.repository.CustomerRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Optional;
//
//@Service
//public class PasswordService {
//
//    @Autowired
//    private CustomerRepository customerRepository;
//
//    @Transactional
//    public boolean updatePassword(PasswordUpdateDTO dto) {
//        Optional<Customer> optionalCustomer = customerRepository.findByCstmId(dto.getCustomerId());
//
//        if (optionalCustomer.isEmpty()) {
//            return false;
//        }
//
//        Customer customer = optionalCustomer.get();
//
//        if (!customer.getPassword().equals(dto.getCurrentPassword())) {
//            return false;
//        }
//
//        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
//            return false;
//        }
//
//        customer.setPassword(dto.getNewPassword());
//        return true;
//    }
//}
//
