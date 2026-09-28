package com.aards.subject;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectFacultyRepository extends JpaRepository<SubjectFaculty, Long> {

    Optional<SubjectFaculty> findBySubjectIdAndAcademicSessionId(Long subjectId, Long sessionId);

    List<SubjectFaculty> findByAcademicSessionId(Long sessionId);
}
