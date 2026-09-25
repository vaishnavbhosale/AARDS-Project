package com.aards.upload;

// Tracks where one PDF upload is in the pipeline.
public enum UploadStatus {
    UPLOADED,
    PARSING,
    PARSED,
    VALIDATED,
    FAILED
}
