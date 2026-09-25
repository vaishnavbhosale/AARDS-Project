package com.aards.recommendation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    List<Recommendation> findByAcademicSessionIdAndDepartmentIdAndYearAndSemester(
            Long sessionId, Long departmentId, Integer year, Integer semester);

    List<Recommendation> findByDepartmentId(Long departmentId);
}
