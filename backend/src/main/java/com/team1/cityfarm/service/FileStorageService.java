package com.team1.cityfarm.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Path BASE_DIR = Paths.get("uploads");

    public String store(MultipartFile file){
        return store(file, "farms");
    }

    public String store(MultipartFile file, String subDir){
        try {
            Path uploadDir = BASE_DIR.resolve(subDir);
            Files.createDirectories(uploadDir);

            // 파일명 중복 장지를 위한 UUID로 변환
            String originalName = file.getOriginalFilename();
            String extension = originalName != null && originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf("."))
                    : "";
            String savedName = UUID.randomUUID() + (extension);

            file.transferTo(uploadDir.resolve(savedName));

            return "/uploads/" + subDir + "/" + savedName;
        } catch (IOException e) {
            throw new UncheckedIOException("이미지 저장에 실패했습니다.", e);
        }
    }
}
