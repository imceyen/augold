package com.augold.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Paths;

@Component
public class PythonScriptRunner {

    @Value("${graph.pythonExecutable.path}")
    private String pythonExecutable;

    @Value("${cart.analysis.python.path}")
    private String scriptPath;

    @EventListener(ApplicationReadyEvent.class)
    public void runPythonScript() {
        try {
            System.out.println("🐍 Python 장바구니 이탈 분석 스크립트 실행 중...");

            // 프로젝트 루트 경로
            String projectRoot = System.getProperty("user.dir");
            String pythonScriptPath = Paths.get(projectRoot, scriptPath).toString();

            // 가상환경의 Python 실행 파일 사용
            ProcessBuilder processBuilder = new ProcessBuilder(pythonExecutable, pythonScriptPath, "1");
            processBuilder.directory(Paths.get(projectRoot, "python", "statistics").toFile());

            Process process = processBuilder.start();

            // 실행 결과 읽기
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("🐍 " + line);
            }

            // 에러 스트림 읽기
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                System.err.println("🐍 Error: " + line);
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("✅ Python 장바구니 이탈 분석 스크립트 실행 완료!");
                System.out.println("📊 JSON 파일이 생성되었습니다!");
            } else {
                System.err.println("❌ Python 스크립트 실행 실패. Exit code: " + exitCode);
            }

        } catch (Exception e) {
            System.err.println("❌ Python 스크립트 실행 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
}