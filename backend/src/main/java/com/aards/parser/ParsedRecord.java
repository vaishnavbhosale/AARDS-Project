package com.aards.parser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

// One student parsed from PDF text. Plain POJO, not an entity.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedRecord {
    private String prn;
    private String rollNumber;
    private String name;
    private int year;
    private int semester;

    @Builder.Default
    private List<SubjectMark> marks = new ArrayList<>();
}
