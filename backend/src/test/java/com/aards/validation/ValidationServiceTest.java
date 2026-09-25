package com.aards.validation;

import com.aards.parser.ParsedRecord;
import com.aards.parser.SubjectMark;
import com.aards.upload.UploadBatch;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Checks the simple validation rules with good and bad records.
@SpringBootTest
class ValidationServiceTest {

    @Autowired
    private ValidationService validationService;

    private UploadBatch fakeBatch() {
        return UploadBatch.builder().id(1L).fileName("test.pdf").build();
    }

    private ParsedRecord validRecord() {
        ParsedRecord record = ParsedRecord.builder()
                .prn("12345678")
                .rollNumber("101")
                .name("Aarav Sharma")
                .year(2)
                .semester(3)
                .build();
        record.getMarks().add(SubjectMark.builder()
                .subjectCode("CS201").subjectName("CS201")
                .marksObtained(75.0).maxMarks(100.0).grade("A").status("PASS").build());
        return record;
    }

    @Test
    void missingPrnGivesError() {
        ParsedRecord record = validRecord();
        record.setPrn("");
        List<ValidationError> errors = validationService.validate(List.of(record), fakeBatch());
        assertTrue(errors.stream().anyMatch(e -> e.getFieldName().equals("PRN")));
    }

    @Test
    void marksAboveMaxGivesError() {
        ParsedRecord record = validRecord();
        record.getMarks().get(0).setMarksObtained(150.0);
        List<ValidationError> errors = validationService.validate(List.of(record), fakeBatch());
        assertTrue(errors.stream().anyMatch(e -> e.getFieldName().equals("CS201")));
    }

    @Test
    void validRecordGivesNoError() {
        List<ValidationError> errors = validationService.validate(List.of(validRecord()), fakeBatch());
        assertEquals(0, errors.size());
    }
}
