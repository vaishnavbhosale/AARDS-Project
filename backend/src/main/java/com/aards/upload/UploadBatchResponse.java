package com.aards.upload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// What frontend sees after an upload. Mapped manually in UploadService.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadBatchResponse {
    private Long id;
    private String fileName;
    private String status;
    private String pdfType;
    private Integer totalRecords;
    private Integer parsedRecords;
    private Integer errorRecords;
    private LocalDateTime uploadedAt;
    private LocalDateTime completedAt;
    private String uploadedByUsername;
    private String departmentName;
    private String academicSessionName;
}
