package com.aards.student.service;

import com.aards.student.StudentRepository;
import com.aards.student.dto.StudentDto;
import com.aards.student.mapper.StudentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

// Simple read service for students.
@Service
public class StudentService {

    private static final Logger log = LoggerFactory.getLogger(StudentService.class);

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;

    public StudentService(StudentRepository studentRepository, StudentMapper studentMapper) {
        this.studentRepository = studentRepository;
        this.studentMapper = studentMapper;
    }

    public List<StudentDto> list() {
        log.info("Fetching student list");
        return studentRepository.findAll().stream().map(studentMapper::toDto).toList();
    }
}
