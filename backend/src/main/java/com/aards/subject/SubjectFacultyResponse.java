package com.aards.subject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Safe view of a subject-faculty assignment. No entity leaks out.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectFacultyResponse {

    private Long id;
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private Long facultyId;
    private String facultyFullName;
    private Long academicSessionId;
    private String sessionName;
}
