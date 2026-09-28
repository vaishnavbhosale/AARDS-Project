package com.aards.subject;

import com.aards.session.AcademicSession;
import com.aards.session.AcademicSessionRepository;
import com.aards.user.User;
import com.aards.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Assigns faculty to subjects per session. Used by the SE Combine report.
@Service
public class SubjectFacultyService {

    private static final Logger log = LoggerFactory.getLogger(SubjectFacultyService.class);

    private final SubjectFacultyRepository subjectFacultyRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final AcademicSessionRepository sessionRepository;

    public SubjectFacultyService(SubjectFacultyRepository subjectFacultyRepository,
                                 SubjectRepository subjectRepository,
                                 UserRepository userRepository,
                                 AcademicSessionRepository sessionRepository) {
        this.subjectFacultyRepository = subjectFacultyRepository;
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public SubjectFacultyResponse assign(SubjectFacultyRequest request) {
        log.info("Assigning faculty {} to subject {} for session {}",
                request.getFacultyId(), request.getSubjectId(), request.getAcademicSessionId());
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found: " + request.getSubjectId()));
        User faculty = userRepository.findById(request.getFacultyId())
                .orElseThrow(() -> new RuntimeException("User not found: " + request.getFacultyId()));
        AcademicSession session = sessionRepository.findById(request.getAcademicSessionId())
                .orElseThrow(() -> new RuntimeException("Session not found: " + request.getAcademicSessionId()));
        if (subjectFacultyRepository
                .findBySubjectIdAndAcademicSessionId(subject.getId(), session.getId()).isPresent()) {
            throw new RuntimeException("Faculty already assigned for this subject and session");
        }
        SubjectFaculty saved = subjectFacultyRepository.save(SubjectFaculty.builder()
                .subject(subject)
                .faculty(faculty)
                .academicSession(session)
                .build());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SubjectFacultyResponse> list(Long academicSessionId) {
        log.info("Listing subject-faculty assignments for session {}", academicSessionId);
        return subjectFacultyRepository.findByAcademicSessionId(academicSessionId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public void delete(Long id) {
        SubjectFaculty assignment = subjectFacultyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found: " + id));
        subjectFacultyRepository.delete(assignment);
        log.info("Deleted subject-faculty assignment {}", id);
    }

    private SubjectFacultyResponse toResponse(SubjectFaculty entity) {
        return SubjectFacultyResponse.builder()
                .id(entity.getId())
                .subjectId(entity.getSubject().getId())
                .subjectCode(entity.getSubject().getCode())
                .subjectName(entity.getSubject().getName())
                .facultyId(entity.getFaculty().getId())
                .facultyFullName(entity.getFaculty().getFullName())
                .academicSessionId(entity.getAcademicSession().getId())
                .sessionName(entity.getAcademicSession().getName())
                .build();
    }
}
