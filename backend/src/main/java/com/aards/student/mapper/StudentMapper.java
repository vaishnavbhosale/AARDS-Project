package com.aards.student.mapper;

import com.aards.student.Student;
import com.aards.student.dto.StudentDto;
import org.springframework.stereotype.Component;

// Converts Student entity to DTO manually so we never return entities directly.
@Component
public class StudentMapper {

    public StudentDto toDto(Student entity) {
        if (entity == null) {
            return null;
        }
        return StudentDto.builder()
                .id(entity.getId())
                .prn(entity.getPrn())
                .rollNumber(entity.getRollNumber())
                .fullName(entity.getFullName())
                .departmentId(entity.getDepartmentId())
                .admissionYear(entity.getAdmissionYear())
                .currentYear(entity.getCurrentYear())
                .currentSemester(entity.getCurrentSemester())
                .build();
    }
}
