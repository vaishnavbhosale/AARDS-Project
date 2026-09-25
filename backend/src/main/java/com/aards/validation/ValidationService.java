package com.aards.validation;

import com.aards.parser.ParsedRecord;
import com.aards.parser.SubjectMark;
import com.aards.upload.UploadBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

// Checks parsed rows for silly mistakes before saving to DB.
// Returns a list of errors for the teacher to fix.
@Service
public class ValidationService {

    private static final Logger log = LoggerFactory.getLogger(ValidationService.class);

    private final ValidationErrorRepository errorRepository;

    public ValidationService(ValidationErrorRepository errorRepository) {
        this.errorRepository = errorRepository;
    }

    public List<ValidationError> validate(List<ParsedRecord> records, UploadBatch batch) {
        log.info("Validation started for batch id={}", batch.getId());
        List<ValidationError> errors = new ArrayList<>();

        for (ParsedRecord record : records) {
            if (record.getPrn() == null || record.getPrn().isBlank()) {
                errors.add(buildError(batch, "-", "prn", String.valueOf(record.getPrn()), "PRN is missing"));
            }
            if (record.getName() == null || record.getName().isBlank()) {
                errors.add(buildError(batch, record.getPrn(), "name", record.getName(), "Name is missing"));
            }
            for (SubjectMark mark : record.getMarks()) {
                if (mark.getMarks() < 0 || mark.getMarks() > mark.getMaxMarks()) {
                    errors.add(buildError(batch, record.getPrn(), mark.getSubjectCode(),
                            mark.getMarks() + "/" + mark.getMaxMarks(), "Marks out of range"));
                }
            }
        }
        log.info("Validation completed, errors={}", errors.size());
        return errors;
    }

    public List<ValidationError> getErrors(Long uploadId) {
        log.info("Fetching validation errors for batch id={}", uploadId);
        return errorRepository.findByUploadBatchId(uploadId);
    }

    private ValidationError buildError(UploadBatch batch, String prn,
                                       String field, String extracted, String hint) {
        return ValidationError.builder()
                .uploadBatchId(batch.getId())
                .studentPrn(prn)
                .fieldName(field)
                .extractedValue(extracted)
                .correctedValue(hint)
                .status(ValidationStatus.PENDING)
                .build();
    }
}
