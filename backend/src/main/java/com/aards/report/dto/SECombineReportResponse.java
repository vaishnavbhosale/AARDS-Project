package com.aards.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// SE Combine report: distribution + backlog + overall + subject tables + toppers.
// Mirrors the college's "SE Combine Main" Excel layout.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SECombineReportResponse {

    private Header header;
    private Distribution distribution;
    private Backlog backlog;
    private Overall overall;

    @Builder.Default
    private List<SemesterBlock> semesters = List.of();

    @Builder.Default
    private List<TopperRow> toppers = List.of();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Header {
        private String collegeName;
        private String departmentName;
        private String sessionName;
        private String yearLabel;
        private String semesterLabel;
        private String generatedOn;
        private long totalAppearedHeader;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountPct {
        private long count;
        private double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Distribution {
        private CountPct distinction;
        private CountPct firstClass;
        private CountPct higherSecond;
        private CountPct secondClass;
        private CountPct passClass;
        // Rare edge case: no SGPA this year and none in prior years either.
        private CountPct unclassified;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Backlog {
        private long failedInOne;
        private long failedInTwo;
        private long failedInThree;
        private long failedInFour;
        private long failedInFiveOrMore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Overall {
        private long totalAppeared;
        private long allClear;
        private double allClearPct;
        private long quality;
        private double qualityPct;
        private long withAtkt;
        private double withAtktPct;
        private long fail;
        private double failPct;
        private long absent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SemesterBlock {
        private int semesterNumber;
        private String semesterDisplayName;
        @Builder.Default
        private List<SubjectRow> subjects = List.of();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectRow {
        private int sn;
        private String subjectCode;
        private String subjectName;
        private String facultyName;
        private long onRoll;
        private long appeared;
        private long passed;
        private double passingPercentage;
        private long distinction;
        private long firstClass;
        private long higherSecond;
        private long secondClass;
        private long passClass;
        private Double highestMarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopperRow {
        private int rank;
        private String name;
        private Double sgpa;
    }
}
