package com.aards.upload;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

// Saves uploaded PDFs to disk. One job: store, load, delete files.
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final Path uploadDir;

    public FileStorageService(@Value("${aards.upload.dir:./uploads}") String dir) {
        this.uploadDir = Paths.get(dir).toAbsolutePath().normalize();
    }

    // Create the folder on startup so store() never fails for missing dir.
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(uploadDir);
            log.info("Upload directory ready: {}", uploadDir);
        } catch (IOException e) {
            log.error("Could not create upload directory", e);
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    public String store(MultipartFile file) {
        log.info("Storing file: {}", file.getOriginalFilename());
        try {
            String name = UUID.randomUUID() + ".pdf";
            Path target = uploadDir.resolve(name);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            log.info("File stored at: {}", target);
            return target.toString();
        } catch (IOException e) {
            log.error("File store failed", e);
            throw new RuntimeException("File store failed", e);
        }
    }

    public Resource load(String path) {
        try {
            Resource resource = new UrlResource(Paths.get(path).toUri());
            if (resource.exists()) {
                return resource;
            }
            throw new RuntimeException("File not found: " + path);
        } catch (Exception e) {
            log.error("File load failed: {}", path, e);
            throw new RuntimeException("File load failed", e);
        }
    }

    public void delete(String path) {
        try {
            boolean removed = Files.deleteIfExists(Paths.get(path));
            log.info("File deleted: {} (existed={})", path, removed);
        } catch (IOException e) {
            log.error("File delete failed: {}", path, e);
            throw new RuntimeException("File delete failed", e);
        }
    }
}
