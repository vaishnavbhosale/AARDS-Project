package com.aards.upload;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UploadBatchRepository extends JpaRepository<UploadBatch, Long> {

    List<UploadBatch> findByUploadedByUserIdOrderByUploadedAtDesc(Long userId);

    List<UploadBatch> findByStatus(UploadStatus status);
}
