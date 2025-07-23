package com.global.augold.admin.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

@Component
public class StreamlitRunner implements CommandLineRunner {

    @Value("${streamlit.script.path}")
    private String scriptPath;

    @Value("${streamlit.executable.path}")
    private String streamlitExecutable;

    private static final Logger log = LoggerFactory.getLogger(StreamlitRunner.class);

    private Process streamlitProcess;

    @Override
    public void run(String... args) throws Exception {
        log.info("Spring Boot 애플리케이션 시작... Streamlit 챗봇 앱을 실행합니다.");

        ProcessBuilder processBuilder = new ProcessBuilder(
                streamlitExecutable,
                "run",
                scriptPath,
                "--server.port", "8501"
        );

        processBuilder.directory(new File(System.getProperty("user.dir")));
        processBuilder.redirectErrorStream(true);

        streamlitProcess = processBuilder.start();
        log.info("✅ Streamlit 챗봇 시작됨: [{}] 포트 8501 (PID: {})", scriptPath, streamlitProcess.pid());

        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(streamlitProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[Streamlit:8501] {}", line);
                }
            } catch (Exception e) {
                log.error("Streamlit 로그 읽기 실패 (스크립트: " + scriptPath + ")", e);
            }
        }).start();

        Runtime.getRuntime().addShutdownHook(new Thread(this::stopStreamlitProcess));
    }

    private void stopStreamlitProcess() {
        if (streamlitProcess != null && streamlitProcess.isAlive()) {
            log.info("🔻 Streamlit 챗봇 종료 중... (PID: {})", streamlitProcess.pid());
            streamlitProcess.destroy();
            try {
                if (!streamlitProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    log.warn("⛔ 정상 종료 실패, 강제 종료 (PID: {})", streamlitProcess.pid());
                    streamlitProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("프로세스 종료 대기 중 인터럽트 발생", e);
            }
        }
        log.info("✅ Streamlit 챗봇 프로세스 종료 완료.");
    }
}
