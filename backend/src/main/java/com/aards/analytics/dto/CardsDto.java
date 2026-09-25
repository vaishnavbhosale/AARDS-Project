package com.aards.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Top number cards on the dashboard.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardsDto {

    @Builder.Default
    private long totalStudents = 0;

    @Builder.Default
    private long passedStudents = 0;

    @Builder.Default
    private long failedStudents = 0;

    @Builder.Default
    private double overallPassPercentage = 0;

    @Builder.Default
    private double averageSgpa = 0;

    @Builder.Default
    private double highestSgpa = 0;

    @Builder.Default
    private double lowestSgpa = 0;

    @Builder.Default
    private long studentsWith1Backlog = 0;

    @Builder.Default
    private long studentsWith2Backlogs = 0;

    @Builder.Default
    private long studentsWith3PlusBacklogs = 0;
}
