package com.aards.parser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// One subject mark inside a ParsedRecord.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectMark {
    private String subjectCode;
    private double marks;
    private double maxMarks;
    private String grade;
}
