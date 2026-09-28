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
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
    // Self-reference through the Spring proxy so @Async actually runs in background.
    private final UploadService self;

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
                         UserRepository userRepository,
                         @Lazy UploadService self) {
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
        this.self = self;
    }

    // Fast checks that fail immediately (bad file, missing setup).
    private static String checkedFileName(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Uploaded file is empty");
        }
        String contentType = file.getContentType();
        String originalName = file.getOriginalFilename() == null ? "result.pdf" : file.getOriginalFilename();
        if (contentType != null && !contentType.equals("application/pdf")
                && !originalName.toLowerCase().endsWith(".pdf")) {
            throw new RuntimeException("Only PDF files are allowed");
        }
        return originalName;
    }

    // Fast department + session check. Throws before anything is stored.
    private Long checkedDepartmentId(User currentUser) {
        Long departmentId = currentUser == null ? null : currentUser.getDepartmentId();
        if (departmentId == null) {
            throw new RuntimeException("Faculty has no department assigned. Contact admin.");
        }
        return departmentId;
    }

    // Sync part: store the file, create the batch, fire the background job.
    // Returns in milliseconds; the frontend polls for progress.
    public UploadBatchResponse submitUpload(MultipartFile file, User currentUser) {
        String originalName = checkedFileName(file);
        Long departmentId = checkedDepartmentId(currentUser);
        AcademicSession session = resolveActiveSession();
        Department department = departmentRepository.findById(departmentId).orElse(null);

        // b. Save file to disk
        String path = fileStorageService.store(file);

        // Read bytes now: the request thread owns the multipart data,
        // the background thread may run after the request is gone.
        final byte[] pdfBytes;
        try {
            pdfBytes = file.getBytes();
        } catch (Exception e) {
            throw new RuntimeException("Could not read uploaded file", e);
        }

        // c. Create batch row as UPLOADED with UNKNOWN type
        UploadBatch batch = UploadBatch.builder()
                .uploadedByUserId(currentUser == null ? null : currentUser.getId())
                .fileName(originalName)
                .filePath(path)
                .status(UploadStatus.UPLOADED)
                .pdfType(PdfType.UNKNOWN)
                .build();
        batch = batchRepository.save(batch);

        // d. Fire and forget: the pipeline continues on a background thread.
        String username = currentUser == null ? "unknown" : currentUser.getUsername();
        log.info("Upload submitted: {} by {}, batch {}", originalName, username, batch.getId());
        self.processUploadAsync(pdfBytes, originalName, batch.getId(),
                currentUser == null ? null : currentUser.getId());

        UploadBatchResponse response = convertToResponse(batch, department, session);
        response.setMessage("Processing in background");
        return response;
    }

    // Slow part: parse -> validate -> save. Runs on the uploadExecutor pool.
    // Any failure marks the batch FAILED so the frontend polling sees it.
    @Async("uploadExecutor")
    @Transactional
    public void processUploadAsync(byte[] pdfBytes, String originalName, Long batchId, Long uploaderId) {
        UploadBatch batch = batchRepository.findById(batchId).orElse(null);
        if (batch == null) {
            log.error("Background upload aborted: batch {} not found", batchId);
            return;
        }
        try {
            User uploader = uploaderId == null ? null
                    : userRepository.findById(uploaderId).orElse(null);
            Long departmentId = checkedDepartmentId(uploader);
            Department department = departmentRepository.findById(departmentId).orElse(null);
            AcademicSession session = resolveActiveSession();

            // e. Mark as PARSING
            batch.setStatus(UploadStatus.PARSING);
            batchRepository.save(batch);

            // f. Parse PDF
            List<ParsedRecord> records = parserService.parseBytes(pdfBytes, originalName);

            // f2. Subject titles from the list page (first page). This must
            // never fail the upload: fall back to codes as names.
            Map<String, String> subjectNames = Map.of();
            try {
                String fullText = parserService.extractFullText(pdfBytes);
                subjectNames = parserService.extractSubjectNames(fullText);
            } catch (Exception e) {
                log.warn("Subject name extraction failed, using codes as names", e);
            }

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
                return;
            }

            // m. No errors: save students + results
            saveParsedData(records, batch, departmentId, session, subjectNames);

            // n. Mark done
            batch.setStatus(UploadStatus.VALIDATED);
            batch.setCompletedAt(LocalDateTime.now());
            batchRepository.save(batch);

            // o. Log done
            log.info("Upload completed: {}, {} records saved", originalName, records.size());

        } catch (Exception e) {
            log.error("Background upload failed for batch {}", batch.getId(), e);
            batch.setStatus(UploadStatus.FAILED);
            String message = e.getMessage() == null ? "Unknown error" : e.getMessage();
            batch.setErrorMessage(message.length() > 1000 ? message.substring(0, 1000) : message);
            batchRepository.save(batch);
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
        byte[] bytes;
        try {
            org.springframework.core.io.Resource resource = fileStorageService.load(batch.getFilePath());
            try (java.io.InputStream in = resource.getInputStream()) {
                bytes = in.readAllBytes();
            }
            records = parserService.parseBytes(bytes, batch.getFileName());
        } catch (Exception e) {
            log.error("Re-parse failed for batch {}", batchId, e);
            throw new RuntimeException("Re-parse failed: " + e.getMessage(), e);
        }
        applyCorrections(records, batchId);
        Map<String, String> subjectNames = Map.of();
        try {
            subjectNames = parserService.extractSubjectNames(
                    parserService.extractFullText(bytes));
        } catch (Exception e) {
            log.warn("Subject name extraction failed, using codes as names", e);
        }
        saveParsedData(records, batch, departmentId, session, subjectNames);
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
    // Batched: students, subjects, results and semester rows each go in one saveAll.
    private void saveParsedData(List<ParsedRecord> records, UploadBatch batch,
                                Long departmentId, AcademicSession session,
                                Map<String, String> subjectNames) {
        int currentYear = LocalDateTime.now().getYear();

        // 1. Students: load the whole department once, upsert in memory.
        Map<String, Student> studentsByPrn = new HashMap<>();
        for (Student s : studentRepository.findByDepartmentId(departmentId)) {
            studentsByPrn.put(s.getPrn(), s);
        }
        List<RecordWork> work = new ArrayList<>();
        for (ParsedRecord record : records) {
            RecordWork w = new RecordWork();
            w.record = record;
            // Group subject rows by their own semester. A row before any
            // SEMESTER line falls back to the record default.
            for (SubjectMark mark : record.getMarks()) {
                int sem = mark.getSemester() == null ? record.getSemester() : mark.getSemester();
                w.bySemester.computeIfAbsent(sem, k -> new ArrayList<>()).add(mark);
            }
            if (w.bySemester.isEmpty()) {
                w.bySemester.put(record.getSemester(), new ArrayList<>());
            }
            // Student row follows the latest semester found in the record.
            int latestSemester = w.bySemester.keySet().stream()
                    .mapToInt(Integer::intValue).max().orElse(record.getSemester());
            int latestYear = (latestSemester + 1) / 2;

            Student student = studentsByPrn.computeIfAbsent(record.getPrn(), prn ->
                    Student.builder().prn(prn).active(true).build());
            student.setFullName(record.getName());
            student.setRollNumber(record.getRollNumber());
            student.setDepartmentId(departmentId);
            student.setCurrentYear(latestYear);
            student.setCurrentSemester(latestSemester);
            student.setAdmissionYear(currentYear - (latestYear - 1));
            w.student = student;
            work.add(w);
        }
        studentRepository.saveAll(work.stream().map(w -> w.student).toList());

        // 2. Subjects: load the whole department once, keyed by code.
        // A subject is one row per (code, department): year/semester come
        // from the record, defaults fill the rest.
        Map<String, Subject> subjectsByKey = new HashMap<>();
        for (Subject s : subjectRepository.findByDepartmentId(departmentId)) {
            subjectsByKey.putIfAbsent(s.getCode(), s);
        }
        List<Subject> newSubjects = new ArrayList<>();
        for (RecordWork w : work) {
            for (Map.Entry<Integer, List<SubjectMark>> entry : w.bySemester.entrySet()) {
                int sem = entry.getKey();
                int year = (sem + 1) / 2;
                for (SubjectMark mark : entry.getValue()) {
                    String key = mark.getSubjectCode();
                    Subject subject = subjectsByKey.get(key);
                    if (subject == null) {
                        subject = Subject.builder()
                                .code(mark.getSubjectCode())
                                .name(resolveSubjectName(mark, subjectNames))
                                .departmentId(departmentId)
                                .year(year)
                                .semester(sem)
                                .credits(mark.getCredits() != null ? mark.getCredits() : 3)
                                .maxMarks(100)
                                .passingMarks(40)
                                .build();
                        subjectsByKey.put(key, subject);
                        newSubjects.add(subject);
                    } else {
                        // Backfill: earlier uploads stored the code as name. Replace it
                        // with the real title once known. Never touch real names.
                        String resolved = resolveSubjectName(mark, subjectNames);
                        if ((subject.getName() == null || subject.getName().equals(subject.getCode()))
                                && !resolved.equals(mark.getSubjectCode())) {
                            subject.setName(resolved);
                        }
                        // Same for credits: fill in the ledger value if unset.
                        if (subject.getCredits() == null && mark.getCredits() != null) {
                            subject.setCredits(mark.getCredits());
                        }
                    }
                }
            }
        }
        subjectRepository.saveAll(newSubjects);

        // 3. Results: build every row first, save in one batch.
        List<Result> results = new ArrayList<>();
        for (RecordWork w : work) {
            for (Map.Entry<Integer, List<SubjectMark>> entry : w.bySemester.entrySet()) {
                int sem = entry.getKey();
                int year = (sem + 1) / 2;
                int backlogs = 0;
                for (SubjectMark mark : entry.getValue()) {
                    Subject subject = subjectsByKey.get(mark.getSubjectCode());
                    double obtained = mark.getMarksObtained() == null ? 0 : mark.getMarksObtained();
                    double max = mark.getMaxMarks() == null ? 100 : mark.getMaxMarks();
                    // Grade decides first: TUT-only rows (A+, A, ...) pass even
                    // when marks sit below the 40% line. Marks are only a
                    // fallback when no grade was parsed.
                    boolean pass = isPassingMark(mark, obtained, max);
                    if (isFailGrade(mark.getGrade())) {
                        backlogs++;
                    }
                    // Parser-flagged absent rows (AAA/AB) keep their own status
                    // so reports bucket them as absent, not failed.
                    boolean absent = "ABSENT".equals(mark.getStatus());
                    results.add(Result.builder()
                            .student(w.student)
                            .subject(subject)
                            .academicSession(session)
                            .year(year)
                            .semester(sem)
                            .marksObtained(obtained)
                            .grade(mark.getGrade())
                            .status(absent ? ResultStatus.ABSENT
                                    : (pass ? ResultStatus.PASS : ResultStatus.FAIL))
                            .backlog(!pass)
                            .build());
                }
                w.backlogsBySemester.put(sem, backlogs);
            }
        }
        resultRepository.saveAll(results);

        // 4. SemesterResults: one per (student, semester). Existing rows for
        // each (year, semester) bucket load once, then upsert in memory.
        Map<String, List<RecordWork>> byYearSem = new LinkedHashMap<>();
        for (RecordWork w : work) {
            for (int sem : w.bySemester.keySet()) {
                int year = (sem + 1) / 2;
                byYearSem.computeIfAbsent(year + "|" + sem, k -> new ArrayList<>()).add(w);
            }
        }
        List<SemesterResult> semesterResults = new ArrayList<>();
        for (Map.Entry<String, List<RecordWork>> bucket : byYearSem.entrySet()) {
            String[] parts = bucket.getKey().split("\\|");
            int year = Integer.parseInt(parts[0]);
            int sem = Integer.parseInt(parts[1]);
            Map<Long, SemesterResult> existingByStudent = new HashMap<>();
            for (SemesterResult sr : semesterResultRepository
                    .findByAcademicSessionIdAndYearAndSemester(session.getId(), year, sem)) {
                existingByStudent.put(sr.getStudentId(), sr);
            }
            for (RecordWork w : bucket.getValue()) {
                final int finalYear = year;
                final int finalSem = sem;
                SemesterResult semesterResult = existingByStudent.computeIfAbsent(
                        w.student.getId(), studentId -> {
                            SemesterResult created = SemesterResult.builder()
                                    .studentId(studentId)
                                    .academicSessionId(session.getId())
                                    .year(finalYear)
                                    .semester(finalSem)
                                    .build();
                            semesterResults.add(created);
                            return created;
                        });
                // SGPA printed in the ledger for this semester, or null if none.
                semesterResult.setSgpa(extractSgpa(w.record, sem));
                int backlogs = w.backlogsBySemester.getOrDefault(sem, 0);
                semesterResult.setBacklogCount(backlogs);
                semesterResult.setStatus(backlogs == 0 ? SemesterStatus.PASS : SemesterStatus.FAIL);
                if (!semesterResults.contains(semesterResult)) {
                    semesterResults.add(semesterResult);
                }
            }
        }
        semesterResultRepository.saveAll(semesterResults);

        // Cleanup: drop auto-created subjects nobody references. Seeded demo
        // rows are never touched.
        List<Subject> orphans = subjectRepository.findByDepartmentId(departmentId).stream()
                .filter(s -> !Boolean.TRUE.equals(s.getSeeded()))
                .filter(s -> !resultRepository.existsBySubjectId(s.getId()))
                .toList();
        if (!orphans.isEmpty()) {
            subjectRepository.deleteAll(orphans);
            log.info("Cleaned up {} unused subjects", orphans.size());
        }
        log.info("Saved {} student records with results", records.size());
    }

    // One record plus its semester groups. Built in step 1, saved in steps 2-4.
    private static class RecordWork {
        ParsedRecord record;
        Student student;
        final Map<Integer, List<SubjectMark>> bySemester = new LinkedHashMap<>();
        final Map<Integer, Integer> backlogsBySemester = new LinkedHashMap<>();
    }

    // F and FFF mean the student failed that subject.
    private boolean isFailGrade(String grade) {
        return "F".equalsIgnoreCase(grade) || "FFF".equalsIgnoreCase(grade);
    }

    // Grade-first pass check. O/A+/A/B+/B/C/P pass regardless of marks
    // (TUT-only subjects); F/FFF fail; null grade falls back to the 40% line.
    private boolean isPassingMark(SubjectMark mark, double obtained, double max) {
        String grade = mark.getGrade();
        if (grade != null) {
            return !isFailGrade(grade);
        }
        return obtained >= 0.4 * max;
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

    // Real title from the PDF list page. Exact code first, then the base code
    // without _PR/_TW. Falls back to the code (old behavior) when unknown.
    private String resolveSubjectName(SubjectMark mark, Map<String, String> subjectNames) {
        String fallback = mark.getSubjectName() == null ? mark.getSubjectCode() : mark.getSubjectName();
        if (subjectNames == null || subjectNames.isEmpty()) {
            return fallback;
        }
        String exact = subjectNames.get(mark.getSubjectCode());
        if (exact != null && !exact.isBlank()) {
            return exact;
        }
        String mapped = subjectNames.get(stripPracticalSuffix(mark.getSubjectCode()));
        if (mapped != null && !mapped.isBlank()) {
            return mapped;
        }
        return fallback;
    }

    // "101011-1_PR" -> "101011-1", "102003_TW" -> "102003".
    private String stripPracticalSuffix(String code) {
        if (code == null) {
            return null;
        }
        if (code.endsWith("_PR") || code.endsWith("_TW")) {
            return code.substring(0, code.length() - 3);
        }
        return code;
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
                .message(batch.getErrorMessage())
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
