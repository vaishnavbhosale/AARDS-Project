package com.aards.parser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// SGPA summary for one semester, read from the ledger SGPA line.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemesterSummary {
    private int semester;
    private Double sgpa;
    private Integer creditsEarned;
    private Integer totalCredits;
    private Integer totalCreditPoints;
}
