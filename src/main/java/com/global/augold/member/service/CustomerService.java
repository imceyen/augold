package com.global.augold.member.service;

import com.global.augold.member.entity.Customer;
import com.global.augold.member.repository.CustomerRepository;
import com.global.augold.member.repository.SequenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {
    @Autowired
    private SequenceRepository sequenceRepository;

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository){
        this.customerRepository = customerRepository;
    }

    public void register(Customer customer){
        // 아이디 중복 확인
        if(customerRepository.existsByCstmId(customer.getCstmId())){
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        String nextId = sequenceRepository.getNextSequence("CUSTOMER");
        customer.setCstmNumber(nextId); // Hibernate가 이 값을 필요로 함
        customerRepository.save(customer);
    }

    // 로그인
    public Customer login(String cstmId, String cstmPwd) {
        return customerRepository.findByCstmIdAndCstmPwd(cstmId, cstmPwd)
                .orElse(null);
    }

}