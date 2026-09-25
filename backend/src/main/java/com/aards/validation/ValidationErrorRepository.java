package com.aards.validation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ValidationErrorRepository extends JpaRepository<ValidationError, Long> {

    List<ValidationError> findByUploadBatchIdAndStatus(Long batchId, ValidationStatus status);

    List<ValidationError> findByUploadBatchId(Long batchId);
}
