package com.aards.recommendation;

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

// AI suggestion for faculty. Example: "Maths pass % is low, take extra lectures".
@Entity
@Table(name = "recommendations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "academic_session_id")
    private Long academicSessionId;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "study_year")
    private Integer year;

    private Integer semester;

    @Column(name = "subject_id")
    private Long subjectId;

    @Column(length = 200)
    private String problem;

    @Column(length = 500)
    private String reason;

    @Column(name = "recommendation_text", columnDefinition = "TEXT")
    private String recommendationText;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
