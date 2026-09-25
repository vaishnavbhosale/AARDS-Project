package com.aards.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// How many results got each grade (O, A+, A, B+, B, C, P, F).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeDistributionDto {

    private String grade;

    @Builder.Default
    private long count = 0;
}
