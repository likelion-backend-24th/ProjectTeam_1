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

    private final Path uploadDir = Paths.get("uploads/farms");

    public String store(MultipartFile file){
        try {
            Files.createDirectories(uploadDir);

            // 파일명 중복 장지를 위한 UUID로 변환
            String originalName = file.getOriginalFilename();
            String extension = originalName != null && originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf("."))
                    : "";
            String savedName = UUID.randomUUID() + (extension);

            file.transferTo(uploadDir.resolve(savedName));

            return "/uploads/farms/" + savedName;
        } catch (IOException e) {
            throw new UncheckedIOException("이미지 저장에 실패했습니다.", e);
        }
    }
}
