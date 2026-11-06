package com.smartclassroom.ui.admin;

import com.smartclassroom.model.AttendanceRecord;
import com.smartclassroom.model.AttendanceSummary;
import com.smartclassroom.model.Student;
import com.smartclassroom.model.Subject;
import com.smartclassroom.model.User;
import com.smartclassroom.service.AttendanceService;
import com.smartclassroom.service.ReportService;
import com.smartclassroom.service.StudentService;
import com.smartclassroom.service.SubjectService;
import com.smartclassroom.ui.common.AppTableModel;
import com.smartclassroom.ui.common.CsvFileChooser;
import com.smartclassroom.ui.common.DateField;
import com.smartclassroom.ui.common.Percent;
import com.smartclassroom.ui.common.Refreshable;
import com.smartclassroom.ui.theme.RoundedPanel;
import com.smartclassroom.ui.theme.Theme;
import com.smartclassroom.ui.theme.UiFactory;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Admin reports: attendance by student / subject / date range, class statistics and CSV export. */
public class ReportsPanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;
    private static final String ALL_STUDENTS = "All students";
    private static final String ALL_SUBJECTS = "All subjects";

    private final transient User actor;
    private final transient AttendanceService attendanceService = new AttendanceService();
    private final transient ReportService reportService = new ReportService();
    private final transient StudentService studentService = new StudentService();
    private final transient SubjectService subjectService = new SubjectService();

    private final JComboBox<Object> studentBox = new JComboBox<>();
    private final JComboBox<Object> subjectBox = new JComboBox<>();
    private final DateField fromField = new DateField();
    private final DateField toField = new DateField();
    private final AppTableModel recordModel =
            new AppTableModel(new String[]{"Date", "Roll No", "Student", "Subject", "Status"});
    private final AppTableModel summaryModel =
            new AppTableModel(new String[]{"Roll No", "Student", "Subject", "Classes", "Attended", "Attendance"});
    private final JTable summaryTable = UiFactory.table(summaryModel);
    private final JComboBox<Subject> statsSubject = new JComboBox<>();
    private final JComboBox<String> statsSection = new JComboBox<>();
    private final JLabel statsResult = UiFactory.muted("Choose a subject and section, then click Show statistics.");
    private transient List<AttendanceRecord> records = new ArrayList<>();
    private transient List<AttendanceSummary> summaries = new ArrayList<>();
    private int threshold = AttendanceService.DEFAULT_THRESHOLD;

    public ReportsPanel(User actor) {
        super(new BorderLayout(0, 16));
        this.actor = actor;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel titles = new JPanel(new BorderLayout());
        titles.setOpaque(false);
        titles.add(UiFactory.title("Reports"), BorderLayout.NORTH);
        titles.add(UiFactory.muted("Attendance reports by student, subject and date range"), BorderLayout.SOUTH);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filters.setOpaque(false);
        filters.add(UiFactory.muted("Student"));
        filters.add(studentBox);
        filters.add(UiFactory.muted("Subject"));
        filters.add(subjectBox);
        filters.add(UiFactory.muted("From"));
        filters.add(fromField);
        filters.add(UiFactory.muted("To"));
        filters.add(toField);
        JButton generate = UiFactory.primaryButton("Generate");
        generate.addActionListener(e -> generate());
        JButton exportRecords = UiFactory.secondaryButton("Export records CSV");
        exportRecords.addActionListener(e -> exportRecords());
        JButton exportSummary = UiFactory.secondaryButton("Export summary CSV");
        exportSummary.addActionListener(e -> exportSummary());
        filters.add(generate);
        filters.add(exportRecords);
        filters.add(exportSummary);

        JPanel north = new JPanel(new BorderLayout(0, 6));
        north.setOpaque(false);
        north.add(titles, BorderLayout.NORTH);
        north.add(filters, BorderLayout.SOUTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Day-by-day records", UiFactory.scroll(UiFactory.table(recordModel)));
        tabs.addTab("Summary per student", UiFactory.scroll(summaryTable));
        UiFactory.highlightRows(summaryTable,
                row -> row < summaries.size() && summaries.get(row).isBelowThreshold(threshold), Theme.DANGER_BG);

        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        statsRow.setOpaque(false);
        statsRow.add(UiFactory.heading("Class statistics"));
        statsRow.add(statsSubject);
        statsRow.add(statsSection);
        JButton showStats = UiFactory.secondaryButton("Show statistics");
        showStats.addActionListener(e -> showStatistics());
        statsRow.add(showStats);
        statsRow.add(statsResult);

        RoundedPanel statsCard = new RoundedPanel(new BorderLayout());
        statsCard.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        statsCard.add(statsRow, BorderLayout.CENTER);

        RoundedPanel center = new RoundedPanel(new BorderLayout(0, 12));
        center.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        center.add(tabs, BorderLayout.CENTER);

        add(north, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(statsCard, BorderLayout.SOUTH);
    }

    @Override
    public void refresh() {
        try {
            studentBox.removeAllItems();
            studentBox.addItem(ALL_STUDENTS);
            List<Student> students = studentService.getAll(actor);
            students.forEach(studentBox::addItem);

            subjectBox.removeAllItems();
            subjectBox.addItem(ALL_SUBJECTS);
            statsSubject.removeAllItems();
            for (Subject s : subjectService.getAll()) {
                subjectBox.addItem(s);
                statsSubject.addItem(s);
            }
            statsSection.removeAllItems();
            new TreeSet<>(students.stream().map(Student::getSection).toList()).forEach(statsSection::addItem);
            threshold = attendanceService.getThreshold();
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        }
    }

    private void generate() {
        try {
            Integer studentId = studentBox.getSelectedItem() instanceof Student s ? s.getStudentId() : null;
            Integer subjectId = subjectBox.getSelectedItem() instanceof Subject s ? s.getSubjectId() : null;
            var from = fromField.getOptionalDate("From date");
            var to = toField.getOptionalDate("To date");
            records = attendanceService.getReportRecords(actor, studentId, subjectId, from, to);
            summaries = attendanceService.getClassSummaries(actor, subjectId, null, from, to).stream()
                    .filter(s -> studentId == null || s.getStudentId() == studentId).toList();

            recordModel.setRowCount(0);
            for (AttendanceRecord r : records) {
                recordModel.addRow(new Object[]{r.getDate(), r.getRollNo(), r.getStudentName(), r.getSubjectName(),
                        r.getStatus()});
            }
            summaryModel.setRowCount(0);
            for (AttendanceSummary s : summaries) {
                summaryModel.addRow(new Object[]{s.getRollNo(), s.getStudentName(), s.getSubjectCode(),
                        s.getTotalClasses(), s.getAttendedClasses(), new Percent(s.getPercentage())});
            }
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        }
    }

    private void exportRecords() {
        try {
            if (records.isEmpty()) {
                UiFactory.warning(this, "There is nothing to export. Click Generate first.");
                return;
            }
            Path file = CsvFileChooser.ask(this, "attendance_records.csv");
            if (file != null) {
                reportService.exportAttendanceRecords(file, records);
                UiFactory.success(this, "Exported " + records.size() + " records to " + file.getFileName());
            }
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        }
    }

    private void exportSummary() {
        try {
            if (summaries.isEmpty()) {
                UiFactory.warning(this, "There is nothing to export. Click Generate first.");
                return;
            }
            Path file = CsvFileChooser.ask(this, "attendance_summary.csv");
            if (file != null) {
                reportService.exportAttendanceSummaries(file, summaries, threshold);
                UiFactory.success(this, "Exported " + summaries.size() + " rows to " + file.getFileName());
            }
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        }
    }

    private void showStatistics() {
        try {
            Subject subject = (Subject) statsSubject.getSelectedItem();
            String section = (String) statsSection.getSelectedItem();
            if (subject == null || section == null) {
                UiFactory.warning(this, "Please choose a subject and a section.");
                return;
            }
            ReportService.ClassStatistics s = reportService.getClassStatistics(actor, subject, section);
            statsResult.setText(s.students() + " students | average attendance " + new Percent(s.averageAttendance())
                    + " | " + s.belowThreshold() + " below " + threshold + "% | average marks "
                    + (s.averageMarksPercent() == null ? "n/a" : new Percent(s.averageMarksPercent())));
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        }
    }
}
