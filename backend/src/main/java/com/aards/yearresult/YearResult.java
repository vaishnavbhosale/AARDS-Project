package com.aards.yearresult;

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

// Official year result per student, exactly as the SPPU ledger declares it
// ("SECOND YEAR Result : ..."). Mirrors SemesterResult: one row per
// (student, session, year). UNKNOWN means the block carried no recognized
// official result; reports then use the explicit credit fallback.
@Entity
@Table(name = "year_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YearResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "academic_session_id", nullable = false)
    private Long academicSessionId;

    @Column(name = "study_year")
    private Integer year;

    @Column(name = "official_result_raw", length = 60)
    private String officialResultRaw;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private YearResultStatus status = YearResultStatus.UNKNOWN;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
