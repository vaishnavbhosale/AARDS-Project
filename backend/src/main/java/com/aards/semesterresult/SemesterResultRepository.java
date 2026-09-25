package com.aards.semesterresult;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SemesterResultRepository extends JpaRepository<SemesterResult, Long> {

    Optional<SemesterResult> findByStudentIdAndAcademicSessionIdAndYearAndSemester(
            Long studentId, Long sessionId, Integer year, Integer semester);

    List<SemesterResult> findByAcademicSessionIdAndYearAndSemester(Long sessionId, Integer year, Integer semester);
}
