package com.global.augold.admin.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.fasterxml.jackson.core.json.JsonReadFeature;


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

    // 기존 관리자 페이지를 렌더링하는 메소드 (이름을 명확히 함)
    @GetMapping("/admin/main")
    public String adminMainPage() {
        return "admin/admin"; // templates/admin/admin.html을 반환
    }

    // 통계 데이터를 JSON으로 제공하는 API 엔드포인트
    @GetMapping("/api/statistics/gold-price-forecast")
    @ResponseBody // 이 어노테이션은 리턴값을 HTML 페이지가 아닌 데이터(JSON)로 반환하게 함
    public ResponseEntity<Map<String, Object>> getGoldPriceForecast() {
        try {

            File outputFile = File.createTempFile("prophet_output_", ".json");

            ProcessBuilder processBuilder = new ProcessBuilder(pythonExecutable, scriptPath, outputFile.getAbsolutePath());
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();


            // 디버깅용 로그 출력
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

            // <<< 여기가 핵심 수정 부분 >>>
            // Jackson ObjectMapper가 비표준 숫자인 NaN을 허용하도록 설정합니다.
            objectMapper.configure(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS.mappedFeature(), true);

            Map<String, Object> data = objectMapper.readValue(jsonData, new TypeReference<>() {});

            return ResponseEntity.ok(data);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "데이터를 불러오는 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}