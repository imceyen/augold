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

    @Value("${graph.scriptPath.path}") // 금 시세 분석 스크립트 경로
    private String goldPriceScriptPath;

    @Value("${graph.salesScriptPath.path}") // 매출 트렌드 분석 스크립트 경로
    private String salesTrendScriptPath;

    // ✅ 관리자 페이지 렌더링
    @GetMapping("/admin/main")
    public String adminMainPage() {
        return "admin/admin"; // templates/admin/admin.html
    }

<<<<<<< HEAD
    // ✅ 금 가격 예측 API - Python 실행 결과 반환
=======
    // --- 금 시세 예측 API (기존 코드) ---
>>>>>>> 11de17da9dc459e44db326e52bfda94cd695d49a
    @GetMapping("/api/statistics/gold-price-forecast")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getGoldPriceForecast() {
        // 기존 금 시세 스크립트를 실행하도록 goldPriceScriptPath 사용
        return executePythonScript(goldPriceScriptPath);
    }

    // --- 매출 트렌드 분석 API (새로 추가된 코드) ---
    @GetMapping("/api/statistics/order-trends") // HTML에서 호출하는 경로와 일치
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOrderTrends() {
        // 새로 추가한 매출 분석 스크립트를 실행하도록 salesTrendScriptPath 사용
        return executePythonScript(salesTrendScriptPath);
    }


    // --- 중복 코드 제거를 위한 공통 메소드 ---
    private ResponseEntity<Map<String, Object>> executePythonScript(String scriptPath) {
        try {
<<<<<<< HEAD
            File outputFile = File.createTempFile("prophet_output_", ".json");

            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonExecutable,
                    scriptPath,
                    outputFile.getAbsolutePath()
            );
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

=======
            // 임시 출력 파일 생성
            File outputFile = File.createTempFile("stats_output_", ".json");

            // Python 스크립트 실행을 위한 ProcessBuilder 설정
            ProcessBuilder processBuilder = new ProcessBuilder(pythonExecutable, scriptPath, outputFile.getAbsolutePath());
            processBuilder.redirectErrorStream(true); // 에러 스트림을 표준 출력 스트림으로 리다이렉트

            Process process = processBuilder.start();

            // Python 스크립트의 표준 출력을 읽어 로그로 남김 (디버깅에 매우 유용)
>>>>>>> 11de17da9dc459e44db326e52bfda94cd695d49a
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                System.out.println("--- Python Script Log (" + new File(scriptPath).getName() + ") ---");
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
                System.out.println("--- End of Log ---");
            }

            // 프로세스가 2분 내에 완료되기를 기다림
            boolean finished = process.waitFor(2, TimeUnit.MINUTES);

            // 프로세스가 시간 내에 끝나지 않았거나, 종료 코드가 0(성공)이 아닌 경우 예외 발생
            if (!finished || process.exitValue() != 0) {
                outputFile.delete(); // 임시 파일 삭제
                throw new RuntimeException("Python 스크립트 실행 실패 또는 타임아웃. 스크립트: " + scriptPath);
            }

            // 결과 JSON 파일을 문자열로 읽어옴
            String jsonData = new String(Files.readAllBytes(outputFile.toPath()));
            outputFile.delete(); // 임시 파일 삭제

            ObjectMapper objectMapper = new ObjectMapper();
<<<<<<< HEAD
=======
            // Python의 NaN, Infinity 같은 비표준 숫자 값을 Java에서 파싱할 수 있도록 설정
>>>>>>> 11de17da9dc459e44db326e52bfda94cd695d49a
            objectMapper.configure(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS.mappedFeature(), true);

            // JSON 문자열을 Map<String, Object> 형태로 변환
            Map<String, Object> data = objectMapper.readValue(jsonData, new TypeReference<>() {});
            return ResponseEntity.ok(data);

        } catch (Exception e) {
            e.printStackTrace();
<<<<<<< HEAD
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "데이터를 불러오는 중 오류가 발생했습니다: " + e.getMessage())
            );
=======
            return ResponseEntity.internalServerError().body(Map.of("error", "데이터 분석 중 서버 오류가 발생했습니다: " + e.getMessage()));
>>>>>>> 11de17da9dc459e44db326e52bfda94cd695d49a
        }
    }
}
