package com.aards.config;

import com.aards.department.Department;
import com.aards.department.DepartmentRepository;
import com.aards.session.AcademicSession;
import com.aards.session.AcademicSessionRepository;
import com.aards.subject.Subject;
import com.aards.subject.SubjectFaculty;
import com.aards.subject.SubjectFacultyRepository;
import com.aards.subject.SubjectRepository;
import com.aards.user.Role;
import com.aards.user.User;
import com.aards.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Creates default admin + departments + sessions + sample subjects on startup.
// Each item is checked first, so restarting the app never creates duplicates.
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final SubjectRepository subjectRepository;
    private final SubjectFacultyRepository subjectFacultyRepository;
    private final AcademicSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      DepartmentRepository departmentRepository,
                      SubjectRepository subjectRepository,
                      SubjectFacultyRepository subjectFacultyRepository,
                      AcademicSessionRepository sessionRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.subjectRepository = subjectRepository;
        this.subjectFacultyRepository = subjectFacultyRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        try {
            seedAdmin();
            Map<String, Department> departments = seedDepartments();
            seedSessions();
            seedSubjects(departments);
            seedSubjectFacultyMappings(departments);
        } catch (Exception e) {
            // Never crash startup because of seed data.
            log.error("Data seeding failed, continuing startup", e);
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

    // Five departments, checked by code so re-runs skip existing ones.
    private Map<String, Department> seedDepartments() {
        List<String[]> wanted = List.of(
                new String[]{"COMP", "Computer Engineering"},
                new String[]{"IT", "Information Technology"},
                new String[]{"ENTC", "Electronics and Telecommunication"},
                new String[]{"MECH", "Mechanical Engineering"},
                new String[]{"CIVIL", "Civil Engineering"});
        Map<String, Department> result = new HashMap<>();
        for (String[] entry : wanted) {
            String code = entry[0];
            String name = entry[1];
            Department dept = departmentRepository.findByCode(code)
                    .orElseGet(() -> {
                        Department saved = departmentRepository.save(Department.builder()
                                .name(name)
                                .code(code)
                                .build());
                        log.info("Seeded department: {}", saved.getName());
                        return saved;
                    });
            result.put(code, dept);
        }
        return result;
    }

    // Two sessions, checked by name. Only 2024-25 is active.
    private void seedSessions() {
        seedOneSession("2024-25", true);
        seedOneSession("2023-24", false);
    }

    private void seedOneSession(String name, boolean active) {
        if (sessionRepository.findByName(name).isEmpty()) {
            AcademicSession session = sessionRepository.save(AcademicSession.builder()
                    .name(name)
                    .active(active)
                    .build());
            log.info("Seeded session: {}", session.getName());
        }
    }

    // Demo subjects for COMP only (FE ledger shape). SE subjects are never
    // seeded: real uploads auto-create them from the PDF paper list.
    private void seedSubjects(Map<String, Department> departments) {
        Department comp = departments.get("COMP");
        if (comp != null) {
            seedOneSubject(comp, "CS201", "Data Structures", 4, 2, 3);
            seedOneSubject(comp, "CS202", "DBMS", 4, 2, 3);
            seedOneSubject(comp, "CS203", "EMFT", 3, 2, 3);
        }
    }

    private void seedOneSubject(Department dept, String code, String name,
                                int credits, int year, int semester) {
        if (subjectRepository
                .findByCodeAndDepartmentIdAndYearAndSemester(code, dept.getId(), year, semester)
                .isEmpty()) {
            Subject subject = subjectRepository.save(Subject.builder()
                    .code(code)
                    .name(name)
                    .departmentId(dept.getId())
                    .year(year)
                    .semester(semester)
                    .credits(credits)
                    .maxMarks(100)
                    .passingMarks(40)
                    .seeded(true)
                    .build());
            log.info("Seeded subject: {} for dept {}", subject.getCode(), dept.getCode());
        }
    }

    // Demo faculty mappings so reports show real names instead of "—".
    // Runs when the table is empty; admin can reassign later on the admin page.
    private void seedSubjectFacultyMappings(Map<String, Department> departments) {
        try {
            long existing = subjectFacultyRepository.count();
            log.info("SubjectFaculty rows on startup: {}", existing);
            if (existing > 0) {
                return;
            }
            User faculty = userRepository.findByUsername("thvaishnav").orElse(null);
            if (faculty == null) {
                log.warn("Skipping subject-faculty seed: user 'thvaishnav' not found");
                return;
            }
            AcademicSession session = sessionRepository.findByActive(true).stream()
                    .findFirst().orElse(null);
            if (session == null) {
                log.warn("Skipping subject-faculty seed: no active session");
                return;
            }
            Department entc = departments.get("ENTC");
            if (entc == null) {
                log.warn("Skipping subject-faculty seed: ENTC department not found");
                return;
            }
            int seeded = 0;
            for (Subject subject : subjectRepository.findByDepartmentId(entc.getId())) {
                if (subjectFacultyRepository
                        .findBySubjectIdAndAcademicSessionId(subject.getId(), session.getId()).isPresent()) {
                    continue;
                }
                subjectFacultyRepository.save(SubjectFaculty.builder()
                        .subject(subject)
                        .faculty(faculty)
                        .academicSession(session)
                        .build());
                seeded++;
            }
            log.info("Seeded {} subject-faculty mappings", seeded);
        } catch (Exception e) {
            log.warn("Subject-faculty seeding failed, continuing startup", e);
        }
    }
}
