package com.aards.parser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// One subject row from the ledger. Plain POJO, not an entity.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectMark {
    private String subjectCode;
    private String subjectSuffix;
    private String subjectName;
    private Double marksObtained;
    private Double maxMarks;
    private String grade;
    private String status;
    // Which semester section this row appeared under. Null when no SEMESTER line seen yet.
    private Integer semester;
}
