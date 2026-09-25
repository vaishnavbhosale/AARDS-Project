package com.aards.subject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectResponse {

    private Long id;
    private String code;
    private String name;
    private Long departmentId;
    private Integer year;
    private Integer semester;
    private Integer credits;
    private Integer maxMarks;
    private Integer passingMarks;
}
