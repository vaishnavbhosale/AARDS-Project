package com.aards.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// Full dashboard in one object: cards + charts + topper.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private CardsDto cards;

    @Builder.Default
    private List<SubjectPerformanceDto> subjectPerformance = List.of();

    @Builder.Default
    private List<BacklogDistributionDto> backlogDistribution = List.of();

    @Builder.Default
    private List<GradeDistributionDto> gradeDistribution = List.of();

    private String topperName;
    private Double topperSgpa;
}
