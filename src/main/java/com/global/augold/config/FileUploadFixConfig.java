/*
package com.global.augold.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileUploadFixConfig {

    // 로거(Logger)를 사용하는 것이 더 표준적인 방법입니다.
    private static final Logger log = LoggerFactory.getLogger(FileUploadFixConfig.class);

    @PostConstruct
    public void fixTomcatAttachmentLimit() {
        String maxCount = "500"; // 테스트를 위해 넉넉한 값으로 설정
        System.setProperty("org.apache.tomcat.util.http.fileupload.FileUploadBase.fileCountMax", maxCount);

        // 서버 시작 시 이 로그가 반드시 보여야 합니다.
        log.info("############################################################");
        log.info("## Tomcat 최대 파일 개수 제한 설정이 적용되었습니다: {}개", maxCount);
        log.info("############################################################");
    }
}*/
