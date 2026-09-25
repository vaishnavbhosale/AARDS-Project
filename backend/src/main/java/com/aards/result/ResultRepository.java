package com.aards.result;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResultRepository extends JpaRepository<Result, Long> {

    List<Result> findByStudentIdAndAcademicSessionId(Long studentId, Long sessionId);

    List<Result> findBySubjectIdAndAcademicSessionId(Long subjectId, Long sessionId);

    long countBySubjectIdAndStatus(Long subjectId, ResultStatus status);
}
