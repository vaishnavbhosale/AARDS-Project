package com.aards.student.mapper;

import com.aards.student.Student;
import com.aards.student.dto.StudentDto;
import org.springframework.stereotype.Component;

@Component
public class StudentMapper {

    public StudentDto toDto(Student entity) {
        if (entity == null) {
            return null;
        }
        return StudentDto.builder()
                .id(entity.getId())
                .seatNo(entity.getSeatNo())
                .name(entity.getName())
                .department(entity.getDepartment())
                .academicYear(entity.getAcademicYear())
                .semester(entity.getSemester())
                .sgpa(entity.getSgpa())
                .result(entity.getResult())
                .build();
    }
}
