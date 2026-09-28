package com.aards.analytics.dto;

import com.aards.yearresult.YearResultStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Test/debug-only trace of the SE Combine student classification.
// Produced by SECombineReportService.reconcileSECombine, which is called
// from tests alone: never by controllers and never by the PDF path.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SECombineReconciliationRow {
    private String prn;
    private String rawOfficialResult;
    private YearResultStatus normalizedOfficialResult;
    private boolean appeared;
    private YearResultStatus finalClassification;
    private boolean fallbackUsed;
}
