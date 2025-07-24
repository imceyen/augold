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

    // ✅ 실행할 Python 경로
    @Value("${graph.pythonExecutable.path}")
    private String pythonExecutable;

    // ✅ 금 시세 예측 Python 스크립트 경로
    @Value("${graph.scriptPath.path}")
    private String goldPriceScriptPath;

    // ✅ 카테고리별 이익률 Python 스크립트 경로
    @Value("${graph.categoryProfitScriptPath.path}")
    private String categoryProfitScriptPath;

    // ✅ 매출 트렌드 분석 Python 스크립트 경로 (💥 이 설정 누락 시 ??? 표시됨)
    @Value("${graph.salesScriptPath.path}")
    private String salesTrendScriptPath;

    // ✅ 성별 연령별 분석 Python 스크립트 경로
    @Value("${graph.sexage.script.path}")
    private String sexAgeScriptPath;

    // ✅ 관리자 페이지 렌더링
    @GetMapping("/admin/main")
    public String adminMainPage() {
        return "admin/admin"; // templates/admin/admin.html
    }



    // ✅ 금 가격 예측 API
    @GetMapping("/api/statistics/gold-price-forecast")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getGoldPriceForecast() {
        return executePythonScript(goldPriceScriptPath);
    }

    // ✅ 카테고리별 이익률 분석 API
    @GetMapping("/api/statistics/category-profit")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCategoryProfit() {
        return executePythonScript(categoryProfitScriptPath);
    }

    // ✅ 매출 트렌드 분석 API
    @GetMapping("/api/statistics/order-trends")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOrderTrends() {
        return executePythonScript(salesTrendScriptPath);
    }

    // ✅ 성별 연령별 분석 API
    @GetMapping("/api/statistics/sex-age-chart")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSexAgeChart() {
        return executePythonScript(sexAgeScriptPath);
    }


    // ✅ 공통 Python 실행 함수
    private ResponseEntity<Map<String, Object>> executePythonScript(String scriptPath) {
        try {
            // 임시 결과 파일 생성
            File outputFile = File.createTempFile("stats_output_", ".json");

            // 프로세스 빌더 설정
            ProcessBuilder processBuilder = new ProcessBuilder(pythonExecutable, scriptPath, outputFile.getAbsolutePath());
            // 실제 실행 명령어 로그 출력
            System.out.println("실행 명령어: " + processBuilder.command());
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // 실행 로그 출력
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                System.out.println("--- Python Script Log (" + new File(scriptPath).getName() + ") ---");
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
                System.out.println("--- End of Log ---");
            }

            // 2분 대기
            boolean finished = process.waitFor(2, TimeUnit.MINUTES);
            if (!finished || process.exitValue() != 0) {
                outputFile.delete();
                throw new RuntimeException("Python 스크립트 실행 실패 또는 타임아웃. 스크립트: " + scriptPath);
            }

            // JSON 결과 파싱
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
}
