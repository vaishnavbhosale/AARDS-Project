package com.aards.upload;

import com.aards.parser.ParsedRecord;
import com.aards.parser.ParserService;
import com.aards.parser.SubjectMark;
import com.aards.result.Result;
import com.aards.result.ResultRepository;
import com.aards.result.ResultStatus;
import com.aards.semesterresult.SemesterResult;
import com.aards.semesterresult.SemesterResultRepository;
import com.aards.semesterresult.SemesterStatus;
import com.aards.session.AcademicSession;
import com.aards.session.AcademicSessionRepository;
import com.aards.student.Student;
import com.aards.student.StudentRepository;
import com.aards.subject.Subject;
import com.aards.subject.SubjectRepository;
import com.aards.user.User;
import com.aards.user.repository.UserRepository;
import com.aards.validation.ValidationError;
import com.aards.validation.ValidationErrorRepository;
import com.aards.validation.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

// The "Orchestrator". One method runs the full pipeline:
// save file -> parse -> validate -> save to DB.
@Service
public class UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadService.class);

    private final FileStorageService fileStorageService;
    private final ParserService parserService;
    private final ValidationService validationService;
    private final UploadBatchRepository batchRepository;
    private final ValidationErrorRepository errorRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final ResultRepository resultRepository;
    private final SemesterResultRepository semesterResultRepository;
    private final AcademicSessionRepository sessionRepository;
    private final UserRepository userRepository;

    public UploadService(FileStorageService fileStorageService,
                         ParserService parserService,
                         ValidationService validationService,
                         UploadBatchRepository batchRepository,
                         ValidationErrorRepository errorRepository,
                         StudentRepository studentRepository,
                         SubjectRepository subjectRepository,
                         ResultRepository resultRepository,
                         SemesterResultRepository semesterResultRepository,
                         AcademicSessionRepository sessionRepository,
                         UserRepository userRepository) {
        this.fileStorageService = fileStorageService;
        this.parserService = parserService;
        this.validationService = validationService;
        this.batchRepository = batchRepository;
        this.errorRepository = errorRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.resultRepository = resultRepository;
        this.semesterResultRepository = semesterResultRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public UploadBatchResponse processUpload(MultipartFile file, User currentUser) {
        // a. File must be a non-empty PDF
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Uploaded file is empty");
        }
        String contentType = file.getContentType();
        String originalName = file.getOriginalFilename() == null ? "result.pdf" : file.getOriginalFilename();
        if (contentType != null && !contentType.equals("application/pdf")
                && !originalName.toLowerCase().endsWith(".pdf")) {
            throw new RuntimeException("Only PDF files are allowed");
        }

        // b. Save file to disk
        String path = fileStorageService.store(file);

        // c. Create batch row as UPLOADED with UNKNOWN type
        UploadBatch batch = UploadBatch.builder()
                .uploadedByUserId(currentUser == null ? null : currentUser.getId())
                .fileName(originalName)
                .filePath(path)
                .status(UploadStatus.UPLOADED)
                .pdfType(PdfType.UNKNOWN)
                .build();
        batch = batchRepository.save(batch);

        // d. Log start
        String username = currentUser == null ? "unknown" : currentUser.getUsername();
        log.info("Upload started: {} by {}", originalName, username);

        try {
            // e. Mark as PARSING
            batch.setStatus(UploadStatus.PARSING);
            batchRepository.save(batch);

            // f. Parse PDF
            List<ParsedRecord> records = parserService.parse(file);

            // g. Record counts
            batch.setTotalRecords(records.size());
            batch.setParsedRecords(records.size());

            // h. PDF type is DIGITAL for now (OCR detection comes later)
            batch.setPdfType(PdfType.DIGITAL);
            batchRepository.save(batch);

            // i. Validate parsed rows
            List<ValidationError> errors = validationService.validate(records, batch);

            // j. Save errors as PENDING
            if (!errors.isEmpty()) {
                errorRepository.saveAll(errors);
            }

            // k. Error count
            batch.setErrorRecords(errors.size());

            // l. Errors found: stop here, teacher fixes them on validation screen
            if (!errors.isEmpty()) {
                batch.setStatus(UploadStatus.PARSED);
                batchRepository.save(batch);
                log.info("Validation errors found: {} in batch {}", errors.size(), batch.getId());
                return convertToResponse(batch);
            }

            // m. No errors: save students + results
            saveParsedData(records, batch);

            // n. Mark done
            batch.setStatus(UploadStatus.VALIDATED);
            batch.setCompletedAt(LocalDateTime.now());
            batchRepository.save(batch);

            // o. Log done
            log.info("Upload completed: {}, {} records saved", originalName, records.size());
            return convertToResponse(batch);

        } catch (Exception e) {
            log.error("Upload failed for batch {}", batch.getId(), e);
            batch.setStatus(UploadStatus.FAILED);
            batchRepository.save(batch);
            throw new RuntimeException("Upload failed: " + e.getMessage(), e);
        }
    }

    // Called after teacher fixes errors and approves the batch.
    @Transactional
    public UploadBatchResponse finalizeBatch(Long batchId) {
        log.info("Finalizing batch {}", batchId);
        UploadBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Upload not found: " + batchId));
        List<ParsedRecord> records;
        try {
            org.springframework.core.io.Resource resource = fileStorageService.load(batch.getFilePath());
            byte[] bytes;
            try (java.io.InputStream in = resource.getInputStream()) {
                bytes = in.readAllBytes();
            }
            records = parserService.parseBytes(bytes, batch.getFileName());
        } catch (Exception e) {
            log.error("Re-parse failed for batch {}", batchId, e);
            throw new RuntimeException("Re-parse failed: " + e.getMessage(), e);
        }
        applyCorrections(records, batchId);
        saveParsedData(records, batch);
        batch.setStatus(UploadStatus.VALIDATED);
        batch.setCompletedAt(LocalDateTime.now());
        batchRepository.save(batch);
        log.info("Upload completed: {}, {} records saved", batch.getFileName(), records.size());
        return convertToResponse(batch);
    }

    // Overwrite doubtful values with teacher-approved corrections.
    private void applyCorrections(List<ParsedRecord> records, Long batchId) {
        List<ValidationError> approved =
                errorRepository.findByUploadBatchIdAndStatus(
                        batchId, com.aards.validation.ValidationStatus.APPROVED);
        for (ValidationError error : approved) {
            if (error.getCorrectedValue() == null) {
                continue;
            }
            for (ParsedRecord record : records) {
                if (record.getPrn() != null && record.getPrn().equals(error.getStudentPrn())) {
                    if ("name".equalsIgnoreCase(error.getFieldName())) {
                        record.setName(error.getCorrectedValue());
                    }
                }
            }
        }
    }

    // Save each parsed student with their subject marks + semester summary.
    private void saveParsedData(List<ParsedRecord> records, UploadBatch batch) {
        AcademicSession session = getOrCreateSession();

        for (ParsedRecord record : records) {
            Student student = studentRepository.findByPrn(record.getPrn())
                    .orElseGet(() -> Student.builder()
                            .prn(record.getPrn())
                            .rollNumber(record.getRollNumber())
                            .fullName(record.getName())
                            .currentYear(record.getYear())
                            .currentSemester(record.getSemester())
                            .active(true)
                            .build());
            student.setFullName(record.getName());
            student.setRollNumber(record.getRollNumber());
            student.setCurrentYear(record.getYear());
            student.setCurrentSemester(record.getSemester());
            student = studentRepository.save(student);

            double totalObtained = 0;
            double totalMax = 0;
            int backlogs = 0;

            for (SubjectMark mark : record.getMarks()) {
                Subject subject = findOrCreateSubject(mark);
                double obtained = mark.getMarksObtained() == null ? 0 : mark.getMarksObtained();
                double max = mark.getMaxMarks() == null ? 100 : mark.getMaxMarks();
                boolean pass = obtained >= 0.4 * max;
                if (!pass) {
                    backlogs++;
                }
                totalObtained += obtained;
                totalMax += max;

                Result result = Result.builder()
                        .student(student)
                        .subject(subject)
                        .academicSession(session)
                        .year(record.getYear())
                        .semester(record.getSemester())
                        .marksObtained(obtained)
                        .grade(mark.getGrade())
                        .status(pass ? ResultStatus.PASS : ResultStatus.FAIL)
                        .backlog(!pass)
                        .build();
                resultRepository.save(result);
            }

            double sgpa = totalMax == 0 ? 0 : (totalObtained / totalMax) * 10.0;
            SemesterResult semesterResult = SemesterResult.builder()
                    .studentId(student.getId())
                    .academicSessionId(session.getId())
                    .year(record.getYear())
                    .semester(record.getSemester())
                    .sgpa(Math.round(sgpa * 100.0) / 100.0)
                    .backlogCount(backlogs)
                    .status(backlogs == 0 ? SemesterStatus.PASS : SemesterStatus.FAIL)
                    .build();
            semesterResultRepository.save(semesterResult);
        }
        log.info("Saved {} student records with results", records.size());
    }

    private Subject findOrCreateSubject(SubjectMark mark) {
        return subjectRepository.findByCodeAndDepartmentIdAndYearAndSemester(
                        mark.getSubjectCode(), null, null, null)
                .orElseGet(() -> subjectRepository.save(Subject.builder()
                        .code(mark.getSubjectCode())
                        .name(mark.getSubjectName() == null ? mark.getSubjectCode() : mark.getSubjectName())
                        .maxMarks(mark.getMaxMarks() == null ? 100 : mark.getMaxMarks().intValue())
                        .passingMarks((int) ((mark.getMaxMarks() == null ? 100 : mark.getMaxMarks()) * 0.4))
                        .credits(4)
                        .build()));
    }

    private AcademicSession getOrCreateSession() {
        return sessionRepository.findByName("2024-25")
                .orElseGet(() -> sessionRepository.save(AcademicSession.builder()
                        .name("2024-25")
                        .active(true)
                        .build()));
    }

    // Manual DTO mapping so entities never go to frontend directly.
    private UploadBatchResponse convertToResponse(UploadBatch batch) {
        String username = null;
        if (batch.getUploadedByUserId() != null) {
            username = userRepository.findById(batch.getUploadedByUserId())
                    .map(User::getUsername).orElse(null);
        }
        return UploadBatchResponse.builder()
                .id(batch.getId())
                .fileName(batch.getFileName())
                .status(batch.getStatus() == null ? null : batch.getStatus().name())
                .pdfType(batch.getPdfType() == null ? null : batch.getPdfType().name())
                .totalRecords(batch.getTotalRecords())
                .parsedRecords(batch.getParsedRecords())
                .errorRecords(batch.getErrorRecords())
                .uploadedAt(batch.getUploadedAt())
                .completedAt(batch.getCompletedAt())
                .uploadedByUsername(username)
                .build();
    }

    public List<UploadBatchResponse> listForUser(Long userId) {
        log.info("Listing uploads for user id={}", userId);
        return batchRepository.findByUploadedByUserIdOrderByUploadedAtDesc(userId)
                .stream().map(this::convertToResponse).toList();
    }

    public List<UploadBatchResponse> listAll() {
        log.info("Listing all uploads");
        return batchRepository.findAll().stream().map(this::convertToResponse).toList();
    }

    public UploadBatchResponse getById(Long id) {
        log.info("Fetching upload batch id={}", id);
        UploadBatch batch = batchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Upload not found: " + id));
        return convertToResponse(batch);
    }
}
