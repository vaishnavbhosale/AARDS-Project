package com.aards.analytics.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Filter coming as query params: ?sessionId=&departmentId=&year=&semester=
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsFilterRequest {

    @NotNull(message = "Session id is required")
    private Long academicSessionId;

    @NotNull(message = "Department id is required")
    private Long departmentId;

    @NotNull(message = "Year is required")
    private Integer year;

    @NotNull(message = "Semester is required")
    private Integer semester;
}
