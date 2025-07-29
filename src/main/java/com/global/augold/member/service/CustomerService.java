package com.global.augold.member.service;

import com.global.augold.member.entity.Customer;
import com.global.augold.member.repository.CustomerRepository;
import com.global.augold.member.repository.SequenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomerService {
    @Autowired
    private SequenceRepository sequenceRepository;

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository){
        this.customerRepository = customerRepository;
    }

    // 회원 가입
    public void register(Customer customer){
        if(customerRepository.existsByCstmId(customer.getCstmId())){
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        String nextId = sequenceRepository.getNextSequence("CUSTOMER");
        customer.setCstmNumber(nextId);
        customerRepository.save(customer);
    }

    // 로그인
    public Customer login(String cstmId, String cstmPwd) {
        return customerRepository.findByCstmIdAndCstmPwd(cstmId, cstmPwd)
                .orElse(null);
    }

    // 현재 비밀번호 확인
    public boolean checkPassword(String cstmNumber, String password) {
        Optional<Customer> optionalCustomer = customerRepository.findById(cstmNumber);
        if (optionalCustomer.isPresent()) {
            Customer customer = optionalCustomer.get();
            return customer.getCstmPwd().equals(password);
        }
        return false;
    }

    // 비밀번호 업데이트
    public void updatePassword(String cstmNumber, String newPassword) {
        Customer customer = customerRepository.findById(cstmNumber)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다: " + cstmNumber));
        customer.setCstmPwd(newPassword);
        customerRepository.save(customer);
    }

    // =================================================================
    // [신규 추가] 연락처 업데이트 메소드
    // =================================================================

    /**
     * 사용자의 연락처를 새로운 연락처로 업데이트합니다.
     * @param cstmNumber 사용자의 고유 번호 (PK)
     * @param newPhone 새로 설정할 연락처
     */
    public void updatePhone(String cstmNumber, String newPhone) {
        // cstmNumber로 DB에서 사용자 정보를 찾습니다. 사용자가 없으면 예외를 발생시킵니다.
        Customer customer = customerRepository.findById(cstmNumber)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다: " + cstmNumber));

        // 찾아낸 Customer 객체의 연락처 필드 값을 newPhone으로 변경합니다.
        customer.setCstmPhone(newPhone);

        // 변경된 연락처 정보를 데이터베이스에 저장(update)합니다.
        customerRepository.save(customer);
    }
}