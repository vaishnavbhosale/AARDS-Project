package com.aards.parser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// One subject mark inside a ParsedRecord. Plain POJO, not an entity.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectMark {
    private String subjectCode;
    private String subjectName;
    private Double marksObtained;
    private Double maxMarks;
    private String grade;
    private String status;
}
