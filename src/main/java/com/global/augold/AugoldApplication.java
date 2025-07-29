package com.global.augold;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AugoldApplication {

	public static void main(String[] args) {
		// DevTools가 비활성화된 상태에서 이 설정이 적용되는지 마지막으로 확인합니다.
		System.setProperty("org.apache.tomcat.util.http.fileupload.FileUploadBase.fileCountMax", "500");
		SpringApplication.run(AugoldApplication.class, args);
	}

}
