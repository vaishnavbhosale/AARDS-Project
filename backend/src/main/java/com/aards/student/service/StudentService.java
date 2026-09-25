package com.aards.student.service;

import com.aards.student.dto.StudentDto;
import com.aards.student.mapper.StudentMapper;
import com.aards.student.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

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
