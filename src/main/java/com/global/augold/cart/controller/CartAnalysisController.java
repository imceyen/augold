package com.global.augold.cart.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Map;
import java.io.File;

@RestController
@RequestMapping("/api/cart-analysis")
public class CartAnalysisController {

    @Value("${graph.pythonExecutable.path}")
    private String pythonExecutable;

    @Value("${cart.analysis.python.path}")
    private String scriptPath;

    @PostMapping("/execute")
    public Map<String, Object> executeAnalysis(@RequestBody Map<String, Object> request) {
        try {
            Integer hours = (Integer) request.get("hours");
            if (hours == null) hours = 1;

            System.out.println(String.format("🐍 Python 장바구니 이탈 분석 실행 (기준: %d시간)", hours));

            String projectRoot = System.getProperty("user.dir");
            String pythonScriptPath = Paths.get(projectRoot, scriptPath).toString();
            File workingDir = Paths.get(projectRoot, "python", "statistics").toFile();

            // 🔥 디버깅: 경로 정보 출력
            System.out.println("📂 Project Root: " + projectRoot);
            System.out.println("📂 Python Executable: " + pythonExecutable);
            System.out.println("📂 Python Script: " + pythonScriptPath);
            System.out.println("📂 Working Directory: " + workingDir.getAbsolutePath());
            System.out.println("📂 Python Script exists: " + new File(pythonScriptPath).exists());
            System.out.println("📂 Python Executable exists: " + new File(pythonExecutable).exists());
            System.out.println("📂 Working Directory exists: " + workingDir.exists());

            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonExecutable, pythonScriptPath, hours.toString()
            );
            processBuilder.directory(workingDir);

            System.out.println("🔥 실행 명령어: " + String.join(" ", processBuilder.command()));

            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                System.out.println("🐍 " + line);
            }

            StringBuilder errorOutput = new StringBuilder();
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
                System.err.println("🐍 Error: " + line);
            }

            int exitCode = process.waitFor();

            System.out.println("🔥 Python 실행 완료. Exit Code: " + exitCode);

            if (exitCode == 0) {
                System.out.println("✅ Python 스크립트 실행 완료!");
                return Map.of(
                        "status", "success",
                        "message", String.format("%d시간 기준 분석이 완료되었습니다.", hours),
                        "hours_threshold", hours,
                        "output", output.toString()
                );
            } else {
                System.err.println("❌ Python 스크립트 실행 실패. Exit code: " + exitCode);
                System.err.println("❌ Error Output: " + errorOutput.toString());
                return Map.of(
                        "status", "error",
                        "message", "Python 스크립트 실행 실패",
                        "hours_threshold", hours,
                        "exitCode", exitCode,
                        "error", errorOutput.toString(),
                        "output", output.toString(),
                        "pythonPath", pythonExecutable,
                        "scriptPath", pythonScriptPath
                );
            }

        } catch (Exception e) {
            System.err.println("❌ Python 스크립트 실행 중 오류: " + e.getMessage());
            e.printStackTrace();
            return Map.of(
                    "status", "error",
                    "message", "Python 스크립트 실행 중 오류가 발생했습니다.",
                    "error", e.getMessage(),
                    "stackTrace", e.toString()
            );
        }
    }
}