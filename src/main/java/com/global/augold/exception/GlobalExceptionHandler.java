package com.global.augold.exception;

import org.apache.tomcat.util.http.fileupload.impl.FileCountLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(org.apache.tomcat.util.http.fileupload.impl.FileCountLimitExceededException.class)
    public ResponseEntity<String> handleFileCountLimitExceeded(org.apache.tomcat.util.http.fileupload.impl.FileCountLimitExceededException ex) {
        String responseMessage = "한 번에 업로드할 수 있는 최대 파일 개수를 초과했습니다.";
        log.error(responseMessage, ex);
        return new ResponseEntity<>(responseMessage, HttpStatus.BAD_REQUEST);
    }

}