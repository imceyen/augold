package com.global.augold.admin.controller;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class AdminStatisticsController {

    @Value("${graph.pythonExecutable.path}")
    private String pythonExecutable;

    @Value("${graph.salesAnalysisScriptPath.path}")
    private String salesAnalysisScriptPath;

    @Value("${graph.scriptPath.path}")
    private String goldPriceScriptPath;

    @Value("${graph.categoryProfitScriptPath.path}")
    private String categoryProfitScriptPath;

    @Value("${graph.salesScriptPath.path}")
    private String salesTrendScriptPath;

    @Value("${graph.sexage.script.path}")
    private String sexAgeScriptPath;

<<<<<<< Updated upstream
=======
    // 관리자 페이지 렌더링
>>>>>>> Stashed changes
    @GetMapping("/admin/main")
    public String adminMainPage() {
        return "admin/admin";
    }

<<<<<<< Updated upstream
    // ✅ 매출 분석 및 예측 API
    @GetMapping("/api/statistics/sales-analysis")
    @ResponseBody
    public ResponseEntity<?> getSalesAnalysis(
            @RequestParam String unit,
            @RequestParam(defaultValue = "false") boolean predict
    ) {
        try {
            // freq 파라미터 (Y/M/W/D)
            String freq = switch (unit.toLowerCase()) {
                case "year" -> "Y";
                case "month" -> "M";
                case "week" -> "W";
                case "day" -> "D";
                default -> "M";
            };

            String jsonFileName = String.format("sales_analysis_%s.json", freq);
            String outputPath = "python/statistics/output/" + jsonFileName;

            ProcessBuilder pb = new ProcessBuilder(
                    "python",
                    salesAnalysisScriptPath,
                    freq,
                    String.valueOf(predict)
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor();

            File resultFile = new File(outputPath);
            if (!resultFile.exists()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "분석 결과 파일이 존재하지 않습니다."));
            }

            String json = Files.readString(resultFile.toPath());
            return ResponseEntity.ok(json);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "매출 분석 중 오류 발생: " + e.getMessage()));
        }
    }

    // ✅ 금 시세 예측
=======
    // 금 가격 예측 API
>>>>>>> Stashed changes
    @GetMapping("/api/statistics/gold-price-forecast")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getGoldPriceForecast() {
        return executePythonScript(goldPriceScriptPath);
    }

<<<<<<< Updated upstream
    // ✅ 카테고리별 이익 분석
=======
    // 카테고리별 이익률 분석 API
>>>>>>> Stashed changes
    @GetMapping("/api/statistics/category-profit")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCategoryProfit() {
        return executePythonScript(categoryProfitScriptPath);
    }

<<<<<<< Updated upstream
    // ✅ 주문 트렌드 (매출 흐름)
=======
    // 매출 트렌드 분석 API
>>>>>>> Stashed changes
    @GetMapping("/api/statistics/order-trends")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOrderTrends() {
        return executePythonScript(salesTrendScriptPath);
    }

<<<<<<< Updated upstream
    // ✅ 성/연령별 차트
=======
    // 성별 연령별 분석 API
>>>>>>> Stashed changes
    @GetMapping("/api/statistics/sex-age-chart")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSexAgeChart() {
        return executePythonScript(sexAgeScriptPath);
    }

<<<<<<< Updated upstream
    // ✅ 공통 Python 실행 처리 함수
    private ResponseEntity<Map<String, Object>> executePythonScript(String scriptPath) {
        try {
            File outputFile = File.createTempFile("stats_output_", ".json");

            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonExecutable, scriptPath, outputFile.getAbsolutePath()
            );
=======
    // ← 여기부터 새로 추가하는 부분

    // 매출 분석 및 예측 API (period 파라미터 받기)
    @GetMapping("/api/sales-analysis")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getSalesAnalysis(String period) {
        // period 파라미터: "day", "week", "month", "year" 중 하나 예상

        try {
            // 임시 결과 파일 생성
            File outputFile = File.createTempFile("sales_analysis_", ".json");

            // Python 스크립트 경로 (매출 분석 스크립트 경로를 재사용하거나 새로 지정)
            String scriptPath = salesTrendScriptPath;

            // 프로세스 빌더, period 인자를 Python 스크립트에 넘기도록 수정 필요
            ProcessBuilder processBuilder = new ProcessBuilder(pythonExecutable, scriptPath, outputFile.getAbsolutePath(), period);
            System.out.println("실행 명령어: " + processBuilder.command());
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                System.out.println("--- Python Script Log (" + new File(scriptPath).getName() + ") ---");
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
                System.out.println("--- End of Log ---");
            }

            boolean finished = process.waitFor(2, TimeUnit.MINUTES);
            if (!finished || process.exitValue() != 0) {
                outputFile.delete();
                throw new RuntimeException("Python 스크립트 실행 실패 또는 타임아웃. 스크립트: " + scriptPath);
            }

            String jsonData = new String(Files.readAllBytes(outputFile.toPath()));
            outputFile.delete();

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS.mappedFeature(), true);

            List<Map<String, Object>> data = objectMapper.readValue(jsonData, new TypeReference<>() {});
            return ResponseEntity.ok(data);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(null);
        }
    }

    // ← 여기까지 새로 추가하는 부분

    // 공통 Python 실행 함수
    private ResponseEntity<Map<String, Object>> executePythonScript(String scriptPath) {
        try {
            File outputFile = File.createTempFile("stats_output_", ".json");

            ProcessBuilder processBuilder = new ProcessBuilder(pythonExecutable, scriptPath, outputFile.getAbsolutePath());
>>>>>>> Stashed changes
            System.out.println("실행 명령어: " + processBuilder.command());

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

<<<<<<< Updated upstream
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
=======
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
>>>>>>> Stashed changes
                String line;
                System.out.println("--- Python Script Log (" + new File(scriptPath).getName() + ") ---");
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
                System.out.println("--- End of Log ---");
            }

            boolean finished = process.waitFor(2, TimeUnit.MINUTES);
            if (!finished || process.exitValue() != 0) {
                outputFile.delete();
                throw new RuntimeException("Python 스크립트 실행 실패 또는 타임아웃. 스크립트: " + scriptPath);
            }

            String jsonData = new String(Files.readAllBytes(outputFile.toPath()));
            outputFile.delete();

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS.mappedFeature(), true);

            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;
            System.err.println("=== Python Error Output ===");
            while ((line = errorReader.readLine()) != null) {
                System.err.println(line);
            }

            Map<String, Object> data = objectMapper.readValue(jsonData, new TypeReference<>() {});
            return ResponseEntity.ok(data);


        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "데이터 분석 중 오류가 발생했습니다: " + e.getMessage())
            );


        }
    }
}
