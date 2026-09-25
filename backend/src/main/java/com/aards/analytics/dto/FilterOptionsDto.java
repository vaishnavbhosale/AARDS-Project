package com.aards.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// Dropdown values for the dashboard filter bar.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterOptionsDto {

    @Builder.Default
    private List<SessionOption> sessions = List.of();

    @Builder.Default
    private List<DepartmentOption> departments = List.of();

    @Builder.Default
    private List<Integer> years = List.of(1, 2, 3, 4);

    @Builder.Default
    private List<Integer> semesters = List.of(1, 2, 3, 4, 5, 6, 7, 8);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionOption {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentOption {
        private Long id;
        private String name;
        private String code;
    }
}
