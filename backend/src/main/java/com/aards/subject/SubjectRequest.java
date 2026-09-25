package com.aards.subject;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectRequest {

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    private Long departmentId;

    @NotNull(message = "Year is required")
    private Integer year;

    @NotNull(message = "Semester is required")
    private Integer semester;

    private Integer credits;
    private Integer maxMarks;
    private Integer passingMarks;
}
