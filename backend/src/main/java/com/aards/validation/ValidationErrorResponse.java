package com.aards.validation;

import com.aards.validation.ValidationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// What the teacher sees on the validation screen.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorResponse {
    private Long id;
    private String studentPrn;
    private String studentName;
    private String fieldName;
    private String extractedValue;
    private String correctedValue;
    private ValidationStatus status;
}
