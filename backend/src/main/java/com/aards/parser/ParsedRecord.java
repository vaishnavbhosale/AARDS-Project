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

    @Builder.Default
    private List<SubjectMark> marks = new ArrayList<>();

    @Builder.Default
    private List<SemesterSummary> semesters = new ArrayList<>();
}
