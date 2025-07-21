package com.global.augold.common.util;

import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class FileUploadUtil {

    public static String upload(MultipartFile file, String uploadDir) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String originalFilename = file.getOriginalFilename();
        String uuid = UUID.randomUUID().toString();
        String savedFileName = uuid + "_" + originalFilename;

        String fullPath = uploadDir + File.separator + savedFileName;

        file.transferTo(new File(fullPath));

        return savedFileName; // DB에 저장할 파일명 또는 경로
    }
}
