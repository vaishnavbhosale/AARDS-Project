package com.aards.student;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByPrn(String prn);

    List<Student> findByDepartmentIdAndCurrentYearAndCurrentSemester(Long departmentId, Integer year, Integer semester);

    List<Student> findByDepartmentId(Long departmentId);
}
