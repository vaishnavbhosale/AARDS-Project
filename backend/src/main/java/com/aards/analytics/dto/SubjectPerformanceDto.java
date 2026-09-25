package com.aards.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Pass/fail summary for one subject.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectPerformanceDto {

    private Long subjectId;
    private String subjectCode;
    private String subjectName;

    @Builder.Default
    private long totalStudents = 0;

    @Builder.Default
    private long passedStudents = 0;

    @Builder.Default
    private long failedStudents = 0;

    @Builder.Default
    private double passPercentage = 0;
}
