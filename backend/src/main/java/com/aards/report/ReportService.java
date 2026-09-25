package com.aards.report;

import com.aards.analytics.AnalyticsService;
import com.aards.analytics.dto.AnalyticsFilterRequest;
import com.aards.analytics.dto.CardsDto;
import com.aards.analytics.dto.DashboardResponse;
import com.aards.analytics.dto.GradeDistributionDto;
import com.aards.analytics.dto.SubjectPerformanceDto;
import com.aards.department.Department;
import com.aards.department.DepartmentRepository;
import com.aards.result.Result;
import com.aards.result.ResultRepository;
import com.aards.result.ResultStatus;
import com.aards.session.AcademicSessionRepository;
import com.aards.subject.Subject;
import com.aards.subject.SubjectRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// PDF reports built on top of AnalyticsService. No analytics maths here,
// only layout: title, tables, footer.
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private static final Color GRAY_BORDER = new Color(160, 160, 160);
    private static final Color HEADER_BG = new Color(230, 230, 230);

    private final AnalyticsService analyticsService;
    private final DepartmentRepository departmentRepository;
    private final AcademicSessionRepository sessionRepository;
    private final SubjectRepository subjectRepository;
    private final ResultRepository resultRepository;

    public ReportService(AnalyticsService analyticsService,
                         DepartmentRepository departmentRepository,
                         AcademicSessionRepository sessionRepository,
                         SubjectRepository subjectRepository,
                         ResultRepository resultRepository) {
        this.analyticsService = analyticsService;
        this.departmentRepository = departmentRepository;
        this.sessionRepository = sessionRepository;
        this.subjectRepository = subjectRepository;
        this.resultRepository = resultRepository;
    }

    // Full department report: cards + topper + subject table.
    public byte[] generateDepartmentReport(AnalyticsFilterRequest filter) {
        log.info("Generating department report for {}", filter);
        DashboardResponse dashboard = analyticsService.getDashboard(filter, null);
        String deptName = departmentName(filter.getDepartmentId());
        String sessionName = sessionName(filter.getAcademicSessionId());

        Document document = createDocument();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            addTitle(document, "Department Report");
            addParagraph(document, deptName + " | Session " + sessionName
                    + " | Year " + filter.getYear() + " | Semester " + filter.getSemester());
            addParagraph(document, "Generated on: " + LocalDate.now());

            if (dashboard.getCards() == null || dashboard.getCards().getTotalStudents() == 0) {
                addParagraph(document, "No data available for the selected filters.");
            } else {
                addSectionHeader(document, "Summary");
                addSummaryTable(document, cardsMap(dashboard.getCards()));
                addSectionHeader(document, "Topper");
                addParagraph(document, "Topper: " + topperText(dashboard));
                addSectionHeader(document, "Subject Performance");
                addSubjectTable(document, dashboard.getSubjectPerformance());
            }

            addFooter(document);
            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Report generation failed", e);
        }
        return out.toByteArray();
    }

    // One-subject report: subject info + pass summary + grade table.
    public byte[] generateSubjectReport(Long subjectId, AnalyticsFilterRequest filter) {
        log.info("Generating subject report for subject {} and {}", subjectId, filter);
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found: " + subjectId));

        // Same counts the dashboard table uses: by subject + session.
        long passed = resultRepository.countBySubjectIdAndAcademicSessionIdAndStatus(
                subjectId, filter.getAcademicSessionId(), ResultStatus.PASS);
        long failed = resultRepository.countBySubjectIdAndAcademicSessionIdAndStatus(
                subjectId, filter.getAcademicSessionId(), ResultStatus.FAIL);
        long total = passed + failed;
        double passPct = total == 0 ? 0 : Math.round(passed * 10000.0 / total) / 100.0;

        List<GradeDistributionDto> grades = new ArrayList<>();
        if (total > 0) {
            Map<String, Long> counts = new TreeMap<>();
            for (Result r : resultRepository.findBySubjectIdAndAcademicSessionId(
                    subjectId, filter.getAcademicSessionId())) {
                String grade = r.getGrade() == null || r.getGrade().isBlank() ? "NA" : r.getGrade();
                counts.put(grade, counts.getOrDefault(grade, 0L) + 1);
            }
            for (Map.Entry<String, Long> e : counts.entrySet()) {
                grades.add(GradeDistributionDto.builder().grade(e.getKey()).count(e.getValue()).build());
            }
        }

        Document document = createDocument();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            addTitle(document, "Subject Report");
            addParagraph(document, subject.getName() + " (" + subject.getCode() + ")");
            addParagraph(document, "Generated on: " + LocalDate.now());

            if (total == 0) {
                addParagraph(document, "No data available for the selected filters.");
            } else {
                addSectionHeader(document, "Summary");
                Map<String, String> summary = new LinkedHashMap<>();
                summary.put("Total Students", String.valueOf(total));
                summary.put("Passed", String.valueOf(passed));
                summary.put("Failed", String.valueOf(failed));
                summary.put("Pass %", passPct + "%");
                addSummaryTable(document, summary);
                addSectionHeader(document, "Grade Distribution");
                addGradeTable(document, grades);
            }

            addFooter(document);
            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Report generation failed", e);
        }
        return out.toByteArray();
    }

    // Institute report: one summary row per department.
    public byte[] generateInstituteReport(Long sessionId, Integer year, Integer semester) {
        log.info("Generating institute report for session {}, year {}, semester {}",
                sessionId, year, semester);
        String sessionName = sessionName(sessionId);
        List<Department> departments = departmentRepository.findAll().stream()
                .sorted(Comparator.comparing(Department::getCode,
                        Comparator.nullsLast(String::compareTo)))
                .toList();

        List<String[]> rows = new ArrayList<>();
        long grandTotal = 0;
        for (Department dept : departments) {
            AnalyticsFilterRequest filter = AnalyticsFilterRequest.builder()
                    .academicSessionId(sessionId)
                    .departmentId(dept.getId())
                    .year(year)
                    .semester(semester)
                    .build();
            CardsDto cards = analyticsService.getDashboard(filter, null).getCards();
            grandTotal += cards == null ? 0 : cards.getTotalStudents();
            rows.add(new String[]{
                    dept.getCode() + " - " + dept.getName(),
                    String.valueOf(cards == null ? 0 : cards.getTotalStudents()),
                    (cards == null ? 0 : cards.getOverallPassPercentage()) + "%",
                    String.valueOf(cards == null ? 0 : cards.getAverageSgpa()),
            });
        }

        Document document = createDocument();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            addTitle(document, "Institute Report");
            addParagraph(document, "Session " + sessionName
                    + " | Year " + year + " | Semester " + semester);
            addParagraph(document, "Generated on: " + LocalDate.now());

            if (grandTotal == 0) {
                addParagraph(document, "No data available for the selected filters.");
            } else {
                addSectionHeader(document, "Department Summary");
                addDepartmentTable(document, rows);
            }

            addFooter(document);
            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Report generation failed", e);
        }
        return out.toByteArray();
    }

    // Plain A4 page with 1cm-ish margins.
    private Document createDocument() {
        return new Document(PageSize.A4, 36, 36, 36, 36);
    }

    // Big centered title, 16pt Helvetica bold.
    private void addTitle(Document document, String text) throws DocumentException {
        Font font = new Font(Font.HELVETICA, 16, Font.BOLD);
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(8);
        document.add(p);
    }

    // Left-aligned section header, 16pt Helvetica bold with space above.
    private void addSectionHeader(Document document, String text) throws DocumentException {
        Font font = new Font(Font.HELVETICA, 16, Font.BOLD);
        Paragraph p = new Paragraph(text, font);
        p.setSpacingBefore(12);
        p.setSpacingAfter(6);
        document.add(p);
    }

    // Normal 12pt body paragraph.
    private void addParagraph(Document document, String text) throws DocumentException {
        Paragraph p = new Paragraph(text, new Font(Font.HELVETICA, 12));
        p.setSpacingAfter(4);
        document.add(p);
    }

    // Two columns: metric name + value.
    private void addSummaryTable(Document document, Map<String, String> rows) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.addCell(headerCell("Metric"));
        table.addCell(headerCell("Value"));
        for (Map.Entry<String, String> e : rows.entrySet()) {
            table.addCell(bodyCell(e.getKey()));
            table.addCell(bodyCell(e.getValue()));
        }
        document.add(table);
    }

    // One row per subject: code, name, totals, pass %.
    private void addSubjectTable(Document document, List<SubjectPerformanceDto> subjects) throws DocumentException {
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        for (String h : List.of("Code", "Name", "Total", "Passed", "Failed", "Pass %")) {
            table.addCell(headerCell(h));
        }
        if (subjects != null) {
            for (SubjectPerformanceDto s : subjects) {
                table.addCell(bodyCell(s.getSubjectCode()));
                table.addCell(bodyCell(s.getSubjectName()));
                table.addCell(bodyCell(String.valueOf(s.getTotalStudents())));
                table.addCell(bodyCell(String.valueOf(s.getPassedStudents())));
                table.addCell(bodyCell(String.valueOf(s.getFailedStudents())));
                table.addCell(bodyCell(s.getPassPercentage() + "%"));
            }
        }
        document.add(table);
    }

    // One row per grade: grade + count.
    private void addGradeTable(Document document, List<GradeDistributionDto> grades) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.addCell(headerCell("Grade"));
        table.addCell(headerCell("Students"));
        if (grades != null) {
            for (GradeDistributionDto g : grades) {
                table.addCell(bodyCell(g.getGrade()));
                table.addCell(bodyCell(String.valueOf(g.getCount())));
            }
        }
        document.add(table);
    }

    // One row per department: name, total, pass %, avg SGPA.
    private void addDepartmentTable(Document document, List<String[]> rows) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        for (String h : List.of("Department", "Total Students", "Pass %", "Avg SGPA")) {
            table.addCell(headerCell(h));
        }
        for (String[] row : rows) {
            for (String value : row) {
                table.addCell(bodyCell(value));
            }
        }
        document.add(table);
    }

    // Small centered footer at the end.
    private void addFooter(Document document) throws DocumentException {
        Paragraph p = new Paragraph("Generated by AARDS",
                new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(120, 120, 120)));
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(16);
        document.add(p);
    }

    // Gray header cell, 12pt bold.
    private PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text,
                new Font(Font.HELVETICA, 12, Font.BOLD)));
        styleCell(cell);
        cell.setBackgroundColor(HEADER_BG);
        return cell;
    }

    // Plain body cell, 12pt.
    private PdfPCell bodyCell(String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text == null ? "" : text,
                new Font(Font.HELVETICA, 12)));
        styleCell(cell);
        return cell;
    }

    // Simple 1pt gray border + padding shared by every cell.
    private void styleCell(PdfPCell cell) {
        cell.setBorderWidth(1);
        cell.setBorderColor(GRAY_BORDER);
        cell.setPadding(5);
    }

    private Map<String, String> cardsMap(CardsDto cards) {
        Map<String, String> rows = new LinkedHashMap<>();
        rows.put("Total Students", String.valueOf(cards.getTotalStudents()));
        rows.put("Passed", String.valueOf(cards.getPassedStudents()));
        rows.put("Failed", String.valueOf(cards.getFailedStudents()));
        rows.put("Overall Pass %", cards.getOverallPassPercentage() + "%");
        rows.put("Average SGPA", String.valueOf(cards.getAverageSgpa()));
        rows.put("Highest SGPA", String.valueOf(cards.getHighestSgpa()));
        rows.put("Lowest SGPA", String.valueOf(cards.getLowestSgpa()));
        rows.put("Students with 1 Backlog", String.valueOf(cards.getStudentsWith1Backlog()));
        rows.put("Students with 2 Backlogs", String.valueOf(cards.getStudentsWith2Backlogs()));
        rows.put("Students with 3+ Backlogs", String.valueOf(cards.getStudentsWith3PlusBacklogs()));
        return rows;
    }

    private String topperText(DashboardResponse dashboard) {
        if (dashboard.getTopperName() == null) {
            return "No topper found for the selected filters.";
        }
        return dashboard.getTopperName() + " (SGPA: " + dashboard.getTopperSgpa() + ")";
    }

    private String departmentName(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .map(Department::getName).orElse("Department " + departmentId);
    }

    private String sessionName(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .map(s -> s.getName()).orElse("Session " + sessionId);
    }
}
