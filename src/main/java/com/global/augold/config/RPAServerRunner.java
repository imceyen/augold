package com.global.augold.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class RPAServerRunner {

    @Value("${graph.pythonExecutable.path}")
    private String pythonExecutable;

    @PostConstruct
    public void runRPAServer() {
        try {
            ProcessBuilder pb = new ProcessBuilder(pythonExecutable, "rpa_server.py");
            pb.directory(new File("C:/ncsGlobal/FinalProject/augold/python/receipt")); // 여기까지 포함
            pb.inheritIO(); // 콘솔 출력 보기
            pb.start();
            System.out.println("✅ RPA 서버 실행 완료");
        } catch (IOException e) {
            System.err.println("❌ RPA 서버 실행 실패:");
            e.printStackTrace();
        }
    }
}