package com.aards.validation;

import com.aards.parser.ParsedRecord;
import com.aards.parser.SubjectMark;
import com.aards.upload.UploadBatch;
import com.aards.upload.UploadService;
import com.aards.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Checks parsed rows for mistakes before saving to DB.
// Lazy UploadService avoids a circle (UploadService also calls this service).
@Service
public class ValidationService {

    private static final Logger log = LoggerFactory.getLogger(ValidationService.class);

    // Accepts old numeric PRNs and ledger PRNs like 72332766B (digits + letter).
    private static final String PRN_RULE = "(\\d{8,12}|\\d{8,9}[A-Z])";

    private final ValidationErrorRepository errorRepository;
    private final UploadService uploadService;

    public ValidationService(ValidationErrorRepository errorRepository,
                             @Lazy UploadService uploadService) {
        this.errorRepository = errorRepository;
        this.uploadService = uploadService;
    }

    public List<ValidationError> validate(List<ParsedRecord> records, UploadBatch batch) {
        log.info("Validation started for batch id={}", batch.getId());
        List<ValidationError> errors = new ArrayList<>();

        for (ParsedRecord record : records) {
            String prn = record.getPrn() == null ? "" : record.getPrn().trim();
            String name = record.getName() == null ? "" : record.getName().trim();

            if (prn.isEmpty()) {
                errors.add(buildError(batch, prn, name, "PRN", "", "PRN is missing"));
            } else if (!prn.matches(PRN_RULE)) {
                errors.add(buildError(batch, prn, name, "PRN", prn, "PRN must be 8-12 digits"));
            }
            if (name.isEmpty()) {
                errors.add(buildError(batch, prn, name, "name", name, "Name is missing"));
            }
            if (record.getYear() < 1 || record.getYear() > 4) {
                errors.add(buildError(batch, prn, name, "year",
                        String.valueOf(record.getYear()), "Year must be 1-4"));
            }
            if (record.getSemester() < 1 || record.getSemester() > 8) {
                errors.add(buildError(batch, prn, name, "semester",
                        String.valueOf(record.getSemester()), "Semester must be 1-8"));
            }
            for (SubjectMark mark : record.getMarks()) {
                double obtained = mark.getMarksObtained() == null ? 0 : mark.getMarksObtained();
                double max = mark.getMaxMarks() == null ? 0 : mark.getMaxMarks();
                if (max <= 0) {
                    errors.add(buildError(batch, prn, name, mark.getSubjectCode(),
                            String.valueOf(max), "Max marks must be above 0"));
                } else if (obtained < 0 || obtained > max) {
                    errors.add(buildError(batch, prn, name, mark.getSubjectCode(),
                            obtained + "/" + max, "Marks out of range"));
                }
            }
        }
        log.info("Validation completed, errors={}", errors.size());
        return errors;
    }

    @Transactional
    public ValidationErrorResponse updateValidationError(Long id, String correctedValue, User reviewer) {
        log.info("Updating validation error id={} by {}", id, reviewer.getUsername());
        ValidationError error = errorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Validation error not found: " + id));
        error.setCorrectedValue(correctedValue);
        error.setStatus(ValidationStatus.APPROVED);
        error.setReviewedByUserId(reviewer.getId());
        error.setReviewedAt(LocalDateTime.now());
        error = errorRepository.save(error);
        return toResponse(error);
    }

    @Transactional
    public void approveBatch(Long batchId, User reviewer) {
        log.info("Approving batch {} by {}", batchId, reviewer.getUsername());
        List<ValidationError> pending =
                errorRepository.findByUploadBatchIdAndStatus(batchId, ValidationStatus.PENDING);
        for (ValidationError error : pending) {
            error.setStatus(ValidationStatus.APPROVED);
            error.setReviewedByUserId(reviewer.getId());
            error.setReviewedAt(LocalDateTime.now());
        }
        errorRepository.saveAll(pending);
        uploadService.finalizeBatch(batchId);
        log.info("Batch {} approved and finalized", batchId);
    }

    public List<ValidationErrorResponse> getErrors(Long batchId) {
        log.info("Fetching validation errors for batch id={}", batchId);
        return errorRepository.findByUploadBatchId(batchId).stream().map(this::toResponse).toList();
    }

    private ValidationError buildError(UploadBatch batch, String prn, String name,
                                       String field, String extracted, String hint) {
        return ValidationError.builder()
                .uploadBatchId(batch.getId())
                .studentPrn(prn)
                .studentName(name)
                .fieldName(field)
                .extractedValue(extracted)
                .correctedValue(hint)
                .status(ValidationStatus.PENDING)
                .build();
    }

    private ValidationErrorResponse toResponse(ValidationError error) {
        return ValidationErrorResponse.builder()
                .id(error.getId())
                .studentPrn(error.getStudentPrn())
                .studentName(error.getStudentName())
                .fieldName(error.getFieldName())
                .extractedValue(error.getExtractedValue())
                .correctedValue(error.getCorrectedValue())
                .status(error.getStatus())
                .build();
    }
}
