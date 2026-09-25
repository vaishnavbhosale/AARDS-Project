package com.aards.student.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDto {

    private Long id;
    private String prn;
    private String rollNumber;
    private String fullName;
    private Long departmentId;
    private Integer admissionYear;
    private Integer currentYear;
    private Integer currentSemester;
}
