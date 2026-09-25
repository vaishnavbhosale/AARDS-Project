package com.aards.upload;

import com.aards.parser.ParsedRecord;
import com.aards.parser.ParserService;
import com.aards.parser.SemesterSummary;
import com.aards.parser.SubjectMark;
import com.aards.result.Result;
import com.aards.result.ResultRepository;
import com.aards.result.ResultStatus;
import com.aards.department.Department;
import com.aards.department.DepartmentRepository;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private final DepartmentRepository departmentRepository;
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
                         DepartmentRepository departmentRepository,
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
        this.departmentRepository = departmentRepository;
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

        // b2. Tagging first: every saved row needs a department + session.
        // Fail here (before storing anything) if the faculty is not set up.
        Long departmentId = currentUser == null ? null : currentUser.getDepartmentId();
        if (departmentId == null) {
            throw new RuntimeException("Faculty has no department assigned. Contact admin.");
        }
        Department department = departmentRepository.findById(departmentId).orElse(null);
        AcademicSession session = resolveActiveSession();

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
                return convertToResponse(batch, department, session);
            }

            // m. No errors: save students + results
            saveParsedData(records, batch, departmentId, session);

            // n. Mark done
            batch.setStatus(UploadStatus.VALIDATED);
            batch.setCompletedAt(LocalDateTime.now());
            batchRepository.save(batch);

            // o. Log done
            log.info("Upload completed: {}, {} records saved", originalName, records.size());
            return convertToResponse(batch, department, session);

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
        User uploader = batch.getUploadedByUserId() == null ? null
                : userRepository.findById(batch.getUploadedByUserId()).orElse(null);
        Long departmentId = uploader == null ? null : uploader.getDepartmentId();
        if (departmentId == null) {
            throw new RuntimeException("Faculty has no department assigned. Contact admin.");
        }
        Department department = departmentRepository.findById(departmentId).orElse(null);
        AcademicSession session = resolveActiveSession();
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
        saveParsedData(records, batch, departmentId, session);
        batch.setStatus(UploadStatus.VALIDATED);
        batch.setCompletedAt(LocalDateTime.now());
        batchRepository.save(batch);
        log.info("Upload completed: {}, {} records saved", batch.getFileName(), records.size());
        return convertToResponse(batch, department, session);
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
    // Every row is tagged with the faculty department + active session so the
    // dashboard can find it. Re-uploading the same PRN updates, never duplicates.
    // Each subject row carries its own semester, so one record can span semesters.
    private void saveParsedData(List<ParsedRecord> records, UploadBatch batch,
                                Long departmentId, AcademicSession session) {
        int currentYear = LocalDateTime.now().getYear();

        for (ParsedRecord record : records) {
            // Group subject rows by their own semester. A row before any
            // SEMESTER line falls back to the record default.
            Map<Integer, List<SubjectMark>> bySemester = new LinkedHashMap<>();
            for (SubjectMark mark : record.getMarks()) {
                int sem = mark.getSemester() == null ? record.getSemester() : mark.getSemester();
                bySemester.computeIfAbsent(sem, k -> new ArrayList<>()).add(mark);
            }
            if (bySemester.isEmpty()) {
                bySemester.put(record.getSemester(), new ArrayList<>());
            }

            // Student row follows the latest semester found in the record.
            int latestSemester = bySemester.keySet().stream()
                    .mapToInt(Integer::intValue).max().orElse(record.getSemester());
            int latestYear = (latestSemester + 1) / 2;

            Student student = studentRepository.findByPrn(record.getPrn())
                    .orElseGet(() -> Student.builder()
                            .prn(record.getPrn())
                            .active(true)
                            .build());
            student.setFullName(record.getName());
            student.setRollNumber(record.getRollNumber());
            student.setDepartmentId(departmentId);
            student.setCurrentYear(latestYear);
            student.setCurrentSemester(latestSemester);
            student.setAdmissionYear(currentYear - (latestYear - 1));
            final Student savedStudent = studentRepository.save(student);

            // One Result per subject row + one SemesterResult per semester.
            for (Map.Entry<Integer, List<SubjectMark>> entry : bySemester.entrySet()) {
                int sem = entry.getKey();
                int year = (sem + 1) / 2;
                int backlogs = 0;

                for (SubjectMark mark : entry.getValue()) {
                    Subject subject = findOrCreateSubject(mark, departmentId, year, sem);
                    double obtained = mark.getMarksObtained() == null ? 0 : mark.getMarksObtained();
                    double max = mark.getMaxMarks() == null ? 100 : mark.getMaxMarks();
                    boolean pass = obtained >= 0.4 * max;
                    if (isFailGrade(mark.getGrade())) {
                        backlogs++;
                    }

                    Result result = Result.builder()
                            .student(savedStudent)
                            .subject(subject)
                            .academicSession(session)
                            .year(year)
                            .semester(sem)
                            .marksObtained(obtained)
                            .grade(mark.getGrade())
                            .status(pass ? ResultStatus.PASS : ResultStatus.FAIL)
                            .backlog(!pass)
                            .build();
                    resultRepository.save(result);
                }

                // SGPA printed in the ledger for this semester, or null if none.
                final Double finalSgpa = extractSgpa(record, sem);
                final int finalBacklogs = backlogs;
                final int finalYear = year;
                final int finalSem = sem;
                SemesterResult semesterResult = semesterResultRepository
                        .findByStudentIdAndAcademicSessionIdAndYearAndSemester(
                                savedStudent.getId(), session.getId(), finalYear, finalSem)
                        .orElseGet(() -> SemesterResult.builder()
                                .studentId(savedStudent.getId())
                                .academicSessionId(session.getId())
                                .year(finalYear)
                                .semester(finalSem)
                                .build());
                semesterResult.setSgpa(finalSgpa);
                semesterResult.setBacklogCount(finalBacklogs);
                semesterResult.setStatus(finalBacklogs == 0 ? SemesterStatus.PASS : SemesterStatus.FAIL);
                semesterResultRepository.save(semesterResult);
            }
        }
        log.info("Saved {} student records with results", records.size());
    }

    // F and FFF mean the student failed that subject.
    private boolean isFailGrade(String grade) {
        return "F".equalsIgnoreCase(grade) || "FFF".equalsIgnoreCase(grade);
    }

    // SGPA printed on the ledger SGPA line for the given semester, if any.
    private Double extractSgpa(ParsedRecord record, int semester) {
        if (record.getSemesters() == null) {
            return null;
        }
        for (SemesterSummary summary : record.getSemesters()) {
            if (summary.getSemester() == semester) {
                return summary.getSgpa();
            }
        }
        return null;
    }

    private Subject findOrCreateSubject(SubjectMark mark, Long departmentId,
                                        Integer year, Integer semester) {
        return subjectRepository.findByCodeAndDepartmentIdAndYearAndSemester(
                        mark.getSubjectCode(), departmentId, year, semester)
                .orElseGet(() -> subjectRepository.save(Subject.builder()
                        .code(mark.getSubjectCode())
                        .name(mark.getSubjectName() == null ? mark.getSubjectCode() : mark.getSubjectName())
                        .departmentId(departmentId)
                        .year(year)
                        .semester(semester)
                        .maxMarks(mark.getMaxMarks() == null ? 100 : mark.getMaxMarks().intValue())
                        .passingMarks((int) ((mark.getMaxMarks() == null ? 100 : mark.getMaxMarks()) * 0.4))
                        .credits(4)
                        .build()));
    }

    // Exactly one session must be active. Uploads always use that one.
    private AcademicSession resolveActiveSession() {
        List<AcademicSession> active = sessionRepository.findByActive(true);
        if (active.isEmpty()) {
            throw new RuntimeException("No active academic session found.");
        }
        return active.get(0);
    }

    // Manual DTO mapping so entities never go to frontend directly.
    private UploadBatchResponse convertToResponse(UploadBatch batch) {
        User uploader = batch.getUploadedByUserId() == null ? null
                : userRepository.findById(batch.getUploadedByUserId()).orElse(null);
        Department department = uploader == null || uploader.getDepartmentId() == null ? null
                : departmentRepository.findById(uploader.getDepartmentId()).orElse(null);
        AcademicSession active = sessionRepository.findByActive(true).stream().findFirst().orElse(null);
        return convertToResponse(batch, department, active);
    }

    private UploadBatchResponse convertToResponse(UploadBatch batch, Department department,
                                                  AcademicSession session) {
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
                .departmentName(department == null ? null : department.getName())
                .academicSessionName(session == null ? null : session.getName())
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
