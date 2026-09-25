package com.aards.validation;

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

// One doubtful value found during validation.
// Teacher fixes correctedValue on the validation screen.
@Entity
@Table(name = "validation_errors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "upload_batch_id", nullable = false)
    private Long uploadBatchId;

    @Column(name = "student_prn", length = 50)
    private String studentPrn;

    @Column(name = "field_name", length = 100)
    private String fieldName;

    @Column(name = "extracted_value", length = 500)
    private String extractedValue;

    @Column(name = "corrected_value", length = 500)
    private String correctedValue;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ValidationStatus status = ValidationStatus.PENDING;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
