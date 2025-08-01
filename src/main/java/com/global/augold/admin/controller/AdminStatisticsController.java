package com.global.augold.admin.controller;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
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

    // ✅ [추가된 부분] 순이익 분석 스크립트 경로를 application.properties에서 가져옵니다.
    @Value("${graph.profitAnalysisScript.path}")
    private String profitAnalysisScriptPath;

    @Value("${graph.scriptPath.path}")
    private String goldPriceScriptPath;

    @Value("${graph.categoryProfitScriptPath.path}")
    private String categoryProfitScriptPath;

    @Value("${graph.salesScriptPath.path}")
    private String salesTrendScriptPath;

    @Value("${graph.sexage.script.path}")
    private String sexAgeScriptPath;

    @Value("${rpa.server.path}")
    private String rpaServerPath;

    // ✅ 관리자 페이지
    @GetMapping("/admin/main")
    public String adminMainPage() {
        return "admin/admin";
    }

    // ✅ 매출 분석 API (기존 코드)
    @GetMapping("/api/statistics/sales-analysis")
    @ResponseBody
    public ResponseEntity<?> getSalesAnalysis(
            @RequestParam String unit,
            @RequestParam(defaultValue = "false") boolean predict
    ) {
        try {
            String freq = switch (unit.toLowerCase()) {
                case "year" -> "Y";
                case "month" -> "M";
                case "week" -> "W";
                case "day" -> "D";
                default -> "M";
            };

            String outputFileName = String.format("sales_analysis_%s.json", freq);
            String outputPath = "python/statistics/output/" + outputFileName;

            ProcessBuilder pb = new ProcessBuilder(
                    pythonExecutable,
                    salesAnalysisScriptPath, // 매출 분석 스크립트
                    freq,
                    String.valueOf(predict)
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                System.out.println("=== Python Sales Script Log ===");
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
                System.out.println("================================");
            }

            boolean finished = process.waitFor(2, TimeUnit.MINUTES);
            if (!finished || process.exitValue() != 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Python 스크립트 실행 실패 또는 타임아웃"));
            }

            File resultFile = new File(outputPath);
            if (!resultFile.exists()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "예측 결과 파일이 존재하지 않습니다."));
            }

            String json = Files.readString(resultFile.toPath());
            return ResponseEntity.ok(json);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "매출 분석 중 오류 발생: " + e.getMessage()));
        }
    }

    // ✅ [추가된 부분] 순이익 분석 API
    @GetMapping("/api/statistics/profit-analysis")
    @ResponseBody
    public ResponseEntity<?> getProfitAnalysis(
            @RequestParam String unit,
            @RequestParam(defaultValue = "false") boolean predict
    ) {
        try {
            String freq = switch (unit.toLowerCase()) {
                case "year" -> "Y";
                case "month" -> "M";
                case "week" -> "W";
                case "day" -> "D";
                default -> "M";
            };

            // 결과 파일 이름을 profit_analysis_...json 으로 변경
            String outputFileName = String.format("profit_analysis_%s.json", freq);
            String outputPath = "python/statistics/output/" + outputFileName;

            ProcessBuilder pb = new ProcessBuilder(
                    pythonExecutable,
                    profitAnalysisScriptPath, // 순이익 분석 스크립트
                    freq,
                    String.valueOf(predict)
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                System.out.println("=== Python Profit Script Log ===");
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
                System.out.println("================================");
            }

            boolean finished = process.waitFor(2, TimeUnit.MINUTES);
            if (!finished || process.exitValue() != 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Python 스크립트 실행 실패 또는 타임아웃"));
            }

            File resultFile = new File(outputPath);
            if (!resultFile.exists()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "예측 결과 파일이 존재하지 않습니다."));
            }

            String json = Files.readString(resultFile.toPath());
            return ResponseEntity.ok(json);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "순이익 분석 중 오류 발생: " + e.getMessage()));
        }
    }


    // ✅ 금 시세 예측
    @GetMapping("/api/statistics/gold-price-forecast")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getGoldPriceForecast() {
        return executePythonScript(goldPriceScriptPath);
    }

    // ✅ 카테고리별 이익 분석
    @GetMapping("/api/statistics/category-profit")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCategoryProfit() {
        return executePythonScript(categoryProfitScriptPath);
    }

    // ✅ 매출 트렌드 분석
    @GetMapping("/api/statistics/order-trends")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOrderTrends() {
        return executePythonScript(salesTrendScriptPath);
    }

    // ✅ 성/연령별 분석
    @GetMapping("/api/statistics/sex-age-chart")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSexAgeChart() {
        return executePythonScript(sexAgeScriptPath);
    }

    // ✅ 공통 Python 스크립트 실행 함수
    private ResponseEntity<Map<String, Object>> executePythonScript(String scriptPath) {
        // (이 부분은 수정 없음)
        try {
            File outputFile = File.createTempFile("stats_output_", ".json");

            ProcessBuilder processBuilder = new ProcessBuilder(pythonExecutable, scriptPath, outputFile.getAbsolutePath());
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                System.out.println("--- Python Script Log ---");
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
                System.out.println("--- End of Log ---");
            }

            boolean finished = process.waitFor(2, TimeUnit.MINUTES);
            if (!finished || process.exitValue() != 0) {
                outputFile.delete();
                throw new RuntimeException("Python 스크립트 실행 실패 또는 타임아웃.");
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
                    Map.of("error", "데이터 분석 중 오류가 발생했습니다: " + e.getMessage())
            );
        }
    }
    @GetMapping("/api/rpa/run")
    @ResponseBody
    public ResponseEntity<?> runRpaScript() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    pythonExecutable,  // 보통은 .py 파일 실행 시
                    rpaServerPath      // rpa_serve.py 또는 exe 경로
            );

            pb.redirectErrorStream(true); // 표준 에러 → 표준 출력으로
            Process process = pb.start();

            // 로그 확인용 출력
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                System.out.println("=== RPA 실행 로그 ===");
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
                System.out.println("=======================");
            }

            boolean finished = process.waitFor(2, TimeUnit.MINUTES);
            if (!finished || process.exitValue() != 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "RPA 스크립트 실행 실패 또는 타임아웃"));
            }

            return ResponseEntity.ok(Map.of("message", "RPA 스크립트 실행 완료"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "RPA 실행 중 오류 발생: " + e.getMessage()));
        }
    }

}