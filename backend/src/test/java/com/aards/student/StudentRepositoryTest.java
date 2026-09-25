package com.aards.student;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Saves one student and reads it back by PRN.
@DataJpaTest
class StudentRepositoryTest {

    @Autowired
    private StudentRepository studentRepository;

    @Test
    void saveAndFindByPrn() {
        Student student = Student.builder()
                .prn("99999999")
                .rollNumber("101")
                .fullName("Test Student")
                .admissionYear(2023)
                .currentYear(2)
                .currentSemester(3)
                .active(true)
                .build();
        studentRepository.save(student);

        Optional<Student> found = studentRepository.findByPrn("99999999");

        assertTrue(found.isPresent());
        assertEquals("Test Student", found.get().getFullName());
    }
}
