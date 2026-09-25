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
    private String seatNo;
    private String name;
    private String department;
    private String academicYear;
    private String semester;
    private Double sgpa;
    private String result;
}
