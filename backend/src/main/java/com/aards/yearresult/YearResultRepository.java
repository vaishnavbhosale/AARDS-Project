package com.aards.yearresult;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface YearResultRepository extends JpaRepository<YearResult, Long> {

    Optional<YearResult> findByStudentIdAndAcademicSessionIdAndYear(
            Long studentId, Long sessionId, Integer year);

    List<YearResult> findByAcademicSessionIdAndYear(Long sessionId, Integer year);

    List<YearResult> findByStudentIdAndAcademicSessionId(Long studentId, Long sessionId);
}
