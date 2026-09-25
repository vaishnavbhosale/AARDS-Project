package com.aards.semesterresult;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Semester summary per student. Used for SGPA cards and backlog counts.
@Entity
@Table(name = "semester_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SemesterResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "academic_session_id", nullable = false)
    private Long academicSessionId;

    @Column(name = "study_year")
    private Integer year;

    private Integer semester;

    private Double sgpa;

    @Column(name = "backlog_count")
    @Builder.Default
    private Integer backlogCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private SemesterStatus status = SemesterStatus.PASS;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
