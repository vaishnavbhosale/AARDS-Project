package com.aards.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// How many students have 0, 1, 2 or 3+ backlogs.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacklogDistributionDto {

    private String label;

    @Builder.Default
    private long count = 0;
}
