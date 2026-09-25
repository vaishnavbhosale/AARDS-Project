package com.aards.config;

import com.aards.department.Department;
import com.aards.department.DepartmentRepository;
import com.aards.session.AcademicSession;
import com.aards.session.AcademicSessionRepository;
import com.aards.subject.Subject;
import com.aards.subject.SubjectRepository;
import com.aards.user.Role;
import com.aards.user.User;
import com.aards.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Creates default admin + sample department/subjects on first startup only.
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      DepartmentRepository departmentRepository,
                      SubjectRepository subjectRepository,
                      AcademicSessionRepository sessionRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.subjectRepository = subjectRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        try {
            seedAdmin();
            seedSession();
            seedDepartmentAndSubjects();
        } catch (Exception e) {
            // Never crash startup because of seed data.
            log.error("Data seeding failed, continuing startup", e);
        }
    }

    private void seedSession() {
        if (sessionRepository.count() == 0) {
            sessionRepository.save(AcademicSession.builder()
                    .name("2024-25")
                    .active(true)
                    .build());
            log.info("Default academic session created: 2024-25");
        }
    }

    private void seedAdmin() {
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("System Admin")
                    .email("admin@aards.local")
                    .role(Role.ADMIN)
                    .active(true)
                    .build();
            userRepository.save(admin);
            log.info("Default admin created: admin / admin123 (change on first login)");
        }
    }

    private void seedDepartmentAndSubjects() {
        if (departmentRepository.count() == 0) {
            Department comp = departmentRepository.save(Department.builder()
                    .name("Computer Engineering")
                    .code("COMP")
                    .build());
            log.info("Default department created: {}", comp.getCode());

            subjectRepository.save(Subject.builder()
                    .code("CS201").name("Data Structures")
                    .departmentId(comp.getId()).year(2).semester(3)
                    .credits(4).maxMarks(100).passingMarks(40).build());
            subjectRepository.save(Subject.builder()
                    .code("CS202").name("DBMS")
                    .departmentId(comp.getId()).year(2).semester(3)
                    .credits(4).maxMarks(100).passingMarks(40).build());
            subjectRepository.save(Subject.builder()
                    .code("CS203").name("EMFT")
                    .departmentId(comp.getId()).year(2).semester(3)
                    .credits(3).maxMarks(100).passingMarks(40).build());
            log.info("Sample subjects created for COMP year 2 sem 3");
        }
    }
}
