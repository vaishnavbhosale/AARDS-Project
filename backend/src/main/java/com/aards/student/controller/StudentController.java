package com.aards.student.controller;

import com.aards.common.dto.ApiResponse;
import com.aards.student.dto.StudentDto;
import com.aards.student.service.StudentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StudentDto>>> list() {
        return ResponseEntity.ok(ApiResponse.success("Students fetched", studentService.list()));
    }
}
