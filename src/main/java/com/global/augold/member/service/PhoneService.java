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
//public class PhoneService {
//
//    @Autowired
//    private CustomerRepository customerRepository;
//
//    @Transactional
//    public boolean updatePhone(PhoneUpdateDTO dto) {
//        Optional<Customer> optionalCustomer = customerRepository.findByCustomerId(dto.getCustomerId());
//
//        if (optionalCustomer.isEmpty()) {
//            return false;
//        }
//
//        Customer customer = optionalCustomer.get();
//        customer.setPhone(dto.getNewPhone());
//        customerRepository.save(customer);
//        return true;
//    }
//}
//
