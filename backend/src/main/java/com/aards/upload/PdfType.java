package com.aards.upload;

// What kind of PDF was uploaded. OCR detection comes later, for now we set DIGITAL.
public enum PdfType {
    DIGITAL,
    SCANNED,
    UNKNOWN
}
