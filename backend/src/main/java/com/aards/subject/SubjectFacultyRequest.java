package com.aards.subject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Admin sends this to assign a faculty to a subject for a session.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectFacultyRequest {

    private Long subjectId;
    private Long facultyId;
    private Long academicSessionId;
}
