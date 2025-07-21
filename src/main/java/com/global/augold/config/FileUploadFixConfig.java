package com.global.augold.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileUploadFixConfig {

    @PostConstruct
    public void fixTomcatAttachmentLimit() {
        System.setProperty("org.apache.tomcat.util.http.fileupload.FileUploadBase.fileCountMax", "100");
    }

}
