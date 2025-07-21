package com.global.augold.admin.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Component // 이 클래스를 Spring Bean으로 등록하여 CommandLineRunner로 동작하게 함
public class StreamlitRunner implements CommandLineRunner {

    // ✅ [수정] 여러 Streamlit 스크립트와 포트를 application.properties에서 받아옵니다
    @Value("#{'${streamlit.script.paths}'.split(',')}")
    private List<String> scriptPaths;

    @Value("#{'${streamlit.ports}'.split(',')}")
    private List<String> ports;

    @Value("${streamlit.executable.path}")
    private String streamlitExecutable;

    // 로그를 남기기 위한 Logger 객체
    private static final Logger log = LoggerFactory.getLogger(StreamlitRunner.class);

    // ✅ [수정] 실행 중인 Streamlit 프로세스들을 관리하기 위한 리스트
    private final List<Process> streamlitProcesses = new ArrayList<>();

    @Override
    public void run(String... args) throws Exception {
        log.info("Spring Boot 애플리케이션 시작... 여러 개의 Streamlit 앱을 실행합니다.");

        for (int i = 0; i < scriptPaths.size(); i++) {
            String script = scriptPaths.get(i).trim();
            String port = (i < ports.size()) ? ports.get(i).trim() : String.valueOf(8501 + i); // 포트 부족 시 자동 증가

            // ✅ 2. ProcessBuilder 설정
            ProcessBuilder processBuilder = new ProcessBuilder(
                    streamlitExecutable,
                    "run",
                    script,
                    "--server.port", port
            );

            // ✅ 3. Python 스크립트가 있는 디렉토리를 작업 디렉토리로 설정
            processBuilder.directory(new File(System.getProperty("user.dir"))); // 프로젝트 루트 디렉토리
            processBuilder.redirectErrorStream(true); // ✅ 4. 로그 리디렉션

            // ✅ 5. Streamlit 프로세스 시작!
            Process process = processBuilder.start();
            streamlitProcesses.add(process);
            log.info("✅ Streamlit 앱 시작됨: [{}] 포트 {} (PID: {})", script, port, process.pid());

            // ✅ 6. 로그 출력 비동기 처리
            int finalI = i; // 람다에서 사용하려면 final 또는 effectively final
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("[Streamlit:{}] {}", ports.get(finalI), line);
                    }
                } catch (Exception e) {
                    log.error("Streamlit 로그 읽기 실패 (스크립트: " + script + ")", e);
                }
            }).start();
        }

        // ✅ 7. Spring 애플리케이션이 종료될 때 모든 Streamlit 프로세스를 함께 종료
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopAllStreamlitProcesses));
    }

    /**
     * Spring Boot 애플리케이션 종료 시 모든 Streamlit 프로세스를 강제 종료하는 메소드
     */
    private void stopAllStreamlitProcesses() {
        for (Process process : streamlitProcesses) {
            if (process != null && process.isAlive()) {
                log.info("🔻 Streamlit 프로세스 종료 중... (PID: {})", process.pid());
                process.destroy();
                try {
                    if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        log.warn("⛔ 정상 종료 실패, 강제 종료 (PID: {})", process.pid());
                        process.destroyForcibly();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("프로세스 종료 대기 중 인터럽트 발생", e);
                }
            }
        }
        log.info("✅ 모든 Streamlit 프로세스가 종료되었습니다.");
    }
}
