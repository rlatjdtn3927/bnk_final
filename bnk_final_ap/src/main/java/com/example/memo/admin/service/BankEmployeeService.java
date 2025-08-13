package com.example.memo.admin.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.memo.jpa.entity.admin.BankEmployee;
import com.example.memo.jpa.repository.admin.BankEmployeeRepository;

@Service
public class BankEmployeeService {

    private final BankEmployeeRepository bankEmployeeRepository;

    @Autowired
    public BankEmployeeService(BankEmployeeRepository bankEmployeeRepository) {
        this.bankEmployeeRepository = bankEmployeeRepository;
    }

    public BankEmployee verifyLogin(String username, String password) {
        Optional<BankEmployee> employeeOptional = bankEmployeeRepository.findByUsername(username);

        if (employeeOptional.isEmpty()) {
            return null; // 사용자가 없음
        }
        
        BankEmployee employee = employeeOptional.get();

        // DB의 비밀번호와 입력된 비밀번호를 단순 비교
        if (employee.getPassword().equals(password)) {
            return employee; // 로그인 성공
        }

        return null; // 비밀번호 불일치
    }
}