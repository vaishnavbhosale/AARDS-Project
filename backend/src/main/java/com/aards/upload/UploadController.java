package com.aards.upload;

import com.aards.common.SecurityUtil;
import com.aards.common.dto.ApiResponse;
import com.aards.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// Upload APIs: faculty uploads a class result PDF.
@RestController
@RequestMapping("/api/v1/uploads")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UploadBatchResponse>> upload(@RequestParam("file") MultipartFile file) {
        log.info("Upload request received: {}", file.getOriginalFilename());
        User user = SecurityUtil.getCurrentUser();
        UploadBatchResponse response = uploadService.processUpload(file, user);
        return ResponseEntity.ok(ApiResponse.success("Upload completed successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UploadBatchResponse>>> list() {
        User user = SecurityUtil.getCurrentUser();
        List<UploadBatchResponse> batches;
        if (SecurityUtil.hasRole("ADMIN") || SecurityUtil.hasRole("HOD")) {
            batches = uploadService.listAll();
        } else {
            batches = uploadService.listForUser(user.getId());
        }
        return ResponseEntity.ok(ApiResponse.success("Uploads fetched", batches));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UploadBatchResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Upload fetched", uploadService.getById(id)));
    }
}
