package com.aards.subject;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Optional<Subject> findByCodeAndDepartmentIdAndYearAndSemester(
            String code, Long departmentId, Integer year, Integer semester);

    List<Subject> findByDepartmentIdAndYearAndSemester(Long departmentId, Integer year, Integer semester);
}
