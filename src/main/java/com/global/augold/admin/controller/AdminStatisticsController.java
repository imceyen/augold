package com.global.augold.admin.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class AdminStatisticsController {

    @Value("${graph.pythonExecutable.path}")
    private String pythonExecutable;

    @Value("${graph.scriptPath.path}")
    private String scriptPath;

    // ✅ 관리자 페이지 렌더링
    @GetMapping("/admin/main")
    public String adminMainPage() {
        return "admin/admin"; // templates/admin/admin.html
    }

    // ✅ 금 가격 예측 API - Python 실행 결과 반환
    @GetMapping("/api/statistics/gold-price-forecast")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getGoldPriceForecast() {
        try {
            File outputFile = File.createTempFile("prophet_output_", ".json");

            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonExecutable,
                    scriptPath,
                    outputFile.getAbsolutePath()
            );
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("Python script output: " + line);
                }
            }

            boolean finished = process.waitFor(2, TimeUnit.MINUTES);
            if (!finished || process.exitValue() != 0) {
                outputFile.delete();
                throw new RuntimeException("Python 스크립트 실행 실패 또는 타임아웃");
            }

            String jsonData = new String(Files.readAllBytes(outputFile.toPath()));
            outputFile.delete();

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS.mappedFeature(), true);

            Map<String, Object> data = objectMapper.readValue(jsonData, new TypeReference<>() {});
            return ResponseEntity.ok(data);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "데이터를 불러오는 중 오류가 발생했습니다: " + e.getMessage())
            );
        }
    }
}
