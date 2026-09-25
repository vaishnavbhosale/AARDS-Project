package com.aards.subject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Simple master data for subjects.
@Service
public class SubjectService {

    private static final Logger log = LoggerFactory.getLogger(SubjectService.class);

    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    @Transactional
    public SubjectResponse create(SubjectRequest request) {
        log.info("Creating subject: {}", request.getCode());
        Subject saved = subjectRepository.save(Subject.builder()
                .code(request.getCode())
                .name(request.getName())
                .departmentId(request.getDepartmentId())
                .year(request.getYear())
                .semester(request.getSemester())
                .credits(request.getCredits() == null ? 4 : request.getCredits())
                .maxMarks(request.getMaxMarks() == null ? 100 : request.getMaxMarks())
                .passingMarks(request.getPassingMarks() == null ? 40 : request.getPassingMarks())
                .build());
        log.info("Subject created with id: {}", saved.getId());
        return toResponse(saved);
    }

    public List<SubjectResponse> list(Long departmentId, Integer year, Integer semester) {
        log.info("Listing subjects with filters");
        List<Subject> subjects;
        if (departmentId != null && year != null && semester != null) {
            subjects = subjectRepository.findByDepartmentIdAndYearAndSemester(departmentId, year, semester);
        } else {
            subjects = subjectRepository.findAll();
        }
        return subjects.stream().map(this::toResponse).toList();
    }

    private SubjectResponse toResponse(Subject subject) {
        return SubjectResponse.builder()
                .id(subject.getId())
                .code(subject.getCode())
                .name(subject.getName())
                .departmentId(subject.getDepartmentId())
                .year(subject.getYear())
                .semester(subject.getSemester())
                .credits(subject.getCredits())
                .maxMarks(subject.getMaxMarks())
                .passingMarks(subject.getPassingMarks())
                .build();
    }
}
