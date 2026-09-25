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

    public UploadService(FileStorageService fileStorageService,
                         ParserService parserService,
                         ValidationService validationService,
                         UploadBatchRepository batchRepository,
                         ValidationErrorRepository errorRepository,
                         StudentRepository studentRepository,
                         SubjectRepository subjectRepository,
                         ResultRepository resultRepository,
                         SemesterResultRepository semesterResultRepository,
                         AcademicSessionRepository sessionRepository) {
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
    }

    @Transactional
    public UploadBatchResponse processUpload(MultipartFile file, User user) {
        log.info("Upload started for file: {}", file.getOriginalFilename());

        // a. Save file to disk
        String path = fileStorageService.store(file);

        // b. Create batch row with UPLOADED status
        UploadBatch batch = UploadBatch.builder()
                .uploadedByUserId(user == null ? null : user.getId())
                .fileName(file.getOriginalFilename())
                .filePath(path)
                .status(UploadStatus.UPLOADED)
                .build();
        batch = batchRepository.save(batch);

        try {
            // c. Parse PDF
            batch.setStatus(UploadStatus.PARSING);
            batchRepository.save(batch);
            List<ParsedRecord> records = parserService.parse(file);
            batch.setTotalRecords(records.size());

            // d. Validate
            List<ValidationError> errors = validationService.validate(records, batch);
            batch.setErrorRecords(errors.size());

            // e. If errors, stop here. Teacher fixes them on validation screen.
            if (!errors.isEmpty()) {
                errorRepository.saveAll(errors);
                batch.setStatus(UploadStatus.PARSED);
                batchRepository.save(batch);
                log.info("Upload parsed with errors: batch={}, errors={}", batch.getId(), errors.size());
                return toResponse(batch);
            }

            // f. No errors: save students + results, then analytics summary
            saveRecords(records);
            batch.setStatus(UploadStatus.VALIDATED);
            batch.setCompletedAt(LocalDateTime.now());
            batchRepository.save(batch);
            log.info("Upload completed for batch id={}", batch.getId());
            return toResponse(batch);

        } catch (Exception e) {
            log.error("Upload failed for batch id={}", batch.getId(), e);
            batch.setStatus(UploadStatus.FAILED);
            batchRepository.save(batch);
            throw new RuntimeException("Upload failed: " + e.getMessage(), e);
        }
    }

    // Save each parsed student and their subject marks.
    private void saveRecords(List<ParsedRecord> records) {
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
            student.setCurrentYear(record.getYear());
            student.setCurrentSemester(record.getSemester());
            student = studentRepository.save(student);

            double totalObtained = 0;
            double totalMax = 0;
            int backlogs = 0;

            for (SubjectMark mark : record.getMarks()) {
                Subject subject = findOrCreateSubject(mark);
                boolean pass = mark.getMarks() >= 0.4 * mark.getMaxMarks();
                if (!pass) {
                    backlogs++;
                }
                totalObtained += mark.getMarks();
                totalMax += mark.getMaxMarks();

                Result result = Result.builder()
                        .student(student)
                        .subject(subject)
                        .academicSession(session)
                        .year(record.getYear())
                        .semester(record.getSemester())
                        .marksObtained(mark.getMarks())
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
                        .name(mark.getSubjectCode())
                        .maxMarks((int) mark.getMaxMarks())
                        .passingMarks((int) (mark.getMaxMarks() * 0.4))
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
    private UploadBatchResponse toResponse(UploadBatch batch) {
        return UploadBatchResponse.builder()
                .id(batch.getId())
                .fileName(batch.getFileName())
                .status(batch.getStatus().name())
                .totalRecords(batch.getTotalRecords())
                .errorRecords(batch.getErrorRecords())
                .build();
    }

    public List<UploadBatchResponse> listForUser(Long userId) {
        log.info("Listing uploads for user id={}", userId);
        return batchRepository.findByUploadedByUserIdOrderByUploadedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    public UploadBatchResponse getById(Long id) {
        log.info("Fetching upload batch id={}", id);
        UploadBatch batch = batchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Upload not found: " + id));
        return toResponse(batch);
    }
}
