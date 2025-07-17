package com.global.augold.admin.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

@Component // 이 클래스를 Spring Bean으로 등록하여 CommandLineRunner로 동작하게 함
public class StreamlitRunner implements CommandLineRunner {

    // 1. Streamlit 실행 파일과 스크립트 경로 설정
    // ※ 중요: 경로가 사용자 환경마다 다를 수 있으므로 환경 변수나 application.properties에서 관리하는 것이 좋습니다.
    @Value("${streamlit.executable.path}")
    private String streamlitExecutable;

    @Value("${streamlit.script.path}")
    private String scriptPath;

    // 로그를 남기기 위한 Logger 객체
    private static final Logger log = LoggerFactory.getLogger(StreamlitRunner.class);

    // 실행 중인 Streamlit 프로세스를 관리하기 위한 변수
    private Process streamlitProcess;

    @Override
    public void run(String... args) throws Exception {
        log.info("Spring Boot 애플리케이션 시작... Streamlit 챗봇을 실행합니다.");

        // 2. ProcessBuilder 설정
        ProcessBuilder processBuilder = new ProcessBuilder(
                streamlitExecutable,
                "run",
                scriptPath,
                "--server.port", "8501" // Streamlit이 사용할 포트 (기본값)
        );

        // 3. Python 스크립트가 있는 디렉토리를 작업 디렉토리로 설정
        // 이렇게 하면 스크립트 내에서 사용하는 상대 경로가 올바르게 동작합니다.
        processBuilder.directory(new File(System.getProperty("user.dir"))); // 프로젝트 루트 디렉토리

        // 4. Streamlit 프로세스의 출력을 Spring Boot 콘솔로 리디렉션
        // 이렇게 해야 Streamlit 로그를 IntelliJ 콘솔에서 바로 확인할 수 있습니다.
        processBuilder.redirectErrorStream(true);

        // 5. Streamlit 프로세스 시작!
        streamlitProcess = processBuilder.start();
        log.info("Streamlit 프로세스가 시작되었습니다. (PID: {})", streamlitProcess.pid());


        // 6. Streamlit의 출력을 비동기적으로 읽어서 로그로 출력 (중요!)
        // 이 부분을 실행하지 않으면 버퍼가 꽉 차서 프로세스가 멈출 수 있습니다.
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(streamlitProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[Streamlit] " + line);
                }
            } catch (Exception e) {
                log.error("Streamlit 로그를 읽는 중 오류 발생", e);
            }
        }).start();

        // 7. Spring 애플리케이션이 종료될 때 Streamlit 프로세스도 함께 종료되도록 설정
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopStreamlit));
    }

    /**
     * Spring Boot 애플리케이션 종료 시 Streamlit 프로세스를 강제 종료하는 메소드
     */
    private void stopStreamlit() {
        if (streamlitProcess != null && streamlitProcess.isAlive()) {
            log.info("Spring Boot 애플리케이션 종료... Streamlit 프로세스를 중지합니다.");
            streamlitProcess.destroy(); // 먼저 정상 종료 시도
            try {
                // 프로세스가 완전히 종료될 때까지 잠시 대기
                if (!streamlitProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    log.warn("Streamlit 프로세스가 정상적으로 종료되지 않아 강제 종료합니다.");
                    streamlitProcess.destroyForcibly(); // 강제 종료
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Streamlit 프로세스 종료 대기 중 인터럽트 발생", e);
            }
            log.info("Streamlit 프로세스가 중지되었습니다.");
        }
    }
}