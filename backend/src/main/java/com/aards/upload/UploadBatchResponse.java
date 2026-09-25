package com.aards.upload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// What frontend sees after an upload. Mapped manually in UploadService.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadBatchResponse {
    private Long id;
    private String fileName;
    private String status;
    private Integer totalRecords;
    private Integer errorRecords;
}
