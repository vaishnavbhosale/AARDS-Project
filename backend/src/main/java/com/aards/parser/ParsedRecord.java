package com.aards.parser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

// One student parsed from the SPPU College Ledger. Plain POJO, not an entity.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedRecord {
    private String prn;
    private String rollNumber;
    private String name;
    private String seatNumber;
    private String motherName;
    private int year;
    private int semester;

    @Builder.Default
    private String overallResult = "UNKNOWN";

    // Official year result straight from the ledger ("SECOND YEAR Result : ...").
    // Raw value exactly as printed (null when the block has no Result line),
    // the year word (1-4, null when no year line found) and the year credit
    // totals from the "Total Credits Earned : a/b" trailer (nullable).
    private String officialResultRaw;
    private Integer officialResultYear;
    private Integer officialCreditsEarned;
    private Integer officialTotalCredits;

    @Builder.Default
    private List<SubjectMark> marks = new ArrayList<>();

    @Builder.Default
    private List<SemesterSummary> semesters = new ArrayList<>();
}
