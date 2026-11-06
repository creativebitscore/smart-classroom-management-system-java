package com.smartclassroom.ui.common;

import com.smartclassroom.model.AttendanceSummary;
import com.smartclassroom.model.Faculty;
import com.smartclassroom.model.Role;
import com.smartclassroom.model.Student;
import com.smartclassroom.model.Subject;
import com.smartclassroom.model.User;
import com.smartclassroom.service.AttendanceService;
import com.smartclassroom.service.ReportService;
import com.smartclassroom.service.StudentService;
import com.smartclassroom.service.SubjectService;
import com.smartclassroom.ui.theme.RoundedPanel;
import com.smartclassroom.ui.theme.Theme;
import com.smartclassroom.ui.theme.UiFactory;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Attendance percentages per student and subject. Rows under the threshold are shown in red.
 * Admin sees every subject and can change the threshold; faculty see only the subjects they teach.
 */
public class AttendanceOverviewPanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;
    private static final String ALL_SUBJECTS = "All subjects";
    private static final String ALL_SECTIONS = "All sections";

    private final transient User user;
    private final transient AttendanceService attendanceService = new AttendanceService();
    private final transient SubjectService subjectService = new SubjectService();
    private final transient StudentService studentService = new StudentService();
    private final transient ReportService reportService = new ReportService();

    private final JComboBox<Object> subjectBox = new JComboBox<>();
    private final JComboBox<String> sectionBox = new JComboBox<>();
    private final DateField fromField = new DateField();
    private final DateField toField = new DateField();
    private final JSpinner thresholdSpinner = new JSpinner(new SpinnerNumberModel(75, 1, 100, 1));
    private final JLabel summaryLabel = UiFactory.muted(" ");
    private final AppTableModel model = new AppTableModel(
            new String[]{"Roll No", "Student", "Subject", "Classes", "Attended", "Attendance", "Status"});
    private final JTable table = UiFactory.table(model);
    private transient List<AttendanceSummary> shown = new ArrayList<>();
    private int threshold = AttendanceService.DEFAULT_THRESHOLD;

    public AttendanceOverviewPanel(User user) {
        super(new BorderLayout(0, 16));
        this.user = user;
        boolean isAdmin = user.getRole() == Role.ADMIN;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel titles = new JPanel(new BorderLayout());
        titles.setOpaque(false);
        titles.add(UiFactory.title("Attendance Overview"), BorderLayout.NORTH);
        titles.add(UiFactory.muted("Students below the threshold are highlighted in red"), BorderLayout.SOUTH);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filters.setOpaque(false);
        filters.add(UiFactory.muted("Subject"));
        filters.add(subjectBox);
        filters.add(UiFactory.muted("Section"));
        filters.add(sectionBox);
        filters.add(UiFactory.muted("From"));
        filters.add(fromField);
        filters.add(UiFactory.muted("To"));
        filters.add(toField);
        JButton show = UiFactory.primaryButton("Show");
        show.addActionListener(e -> load());
        filters.add(show);
        JButton export = UiFactory.secondaryButton("Export CSV");
        export.addActionListener(e -> exportCsv());
        filters.add(export);

        JPanel thresholdRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        thresholdRow.setOpaque(false);
        thresholdRow.add(UiFactory.muted("Minimum attendance required (%)"));
        thresholdRow.add(thresholdSpinner);
        if (isAdmin) {
            JButton save = UiFactory.secondaryButton("Save threshold");
            save.addActionListener(e -> saveThreshold());
            thresholdRow.add(save);
        } else {
            thresholdSpinner.setEnabled(false);
        }
        thresholdRow.add(summaryLabel);

        JPanel north = new JPanel(new BorderLayout(0, 6));
        north.setOpaque(false);
        north.add(titles, BorderLayout.NORTH);
        JPanel controls = new JPanel(new BorderLayout());
        controls.setOpaque(false);
        controls.add(filters, BorderLayout.NORTH);
        controls.add(thresholdRow, BorderLayout.SOUTH);
        north.add(controls, BorderLayout.SOUTH);

        RoundedPanel card = new RoundedPanel(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        card.add(UiFactory.scroll(table), BorderLayout.CENTER);

        add(north, BorderLayout.NORTH);
        add(card, BorderLayout.CENTER);
        UiFactory.highlightRows(table, row -> row < shown.size() && shown.get(row).isBelowThreshold(threshold),
                Theme.DANGER_BG);
    }

    @Override
    public void refresh() {
        try {
            Object keepSubject = subjectBox.getSelectedItem();
            subjectBox.removeAllItems();
            List<Subject> subjects = user.getRole() == Role.ADMIN
                    ? subjectService.getAll() : subjectService.getTaughtBy((Faculty) user);
            if (user.getRole() == Role.ADMIN) {
                subjectBox.addItem(ALL_SUBJECTS);
            }
            subjects.forEach(subjectBox::addItem);
            if (keepSubject != null) {
                subjectBox.setSelectedItem(keepSubject);
            }

            sectionBox.removeAllItems();
            sectionBox.addItem(ALL_SECTIONS);
            new TreeSet<>(studentService.getAll(user).stream().map(Student::getSection).toList())
                    .forEach(sectionBox::addItem);

            threshold = attendanceService.getThreshold();
            thresholdSpinner.setValue(threshold);
            load();
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        }
    }

    private void load() {
        try {
            Object chosen = subjectBox.getSelectedItem();
            Integer subjectId = chosen instanceof Subject s ? s.getSubjectId() : null;
            String section = ALL_SECTIONS.equals(sectionBox.getSelectedItem()) ? null : (String) sectionBox.getSelectedItem();
            shown = attendanceService.getClassSummaries(user, subjectId, section,
                    fromField.getOptionalDate("From date"), toField.getOptionalDate("To date"));
            model.setRowCount(0);
            int below = 0;
            for (AttendanceSummary s : shown) {
                boolean low = s.isBelowThreshold(threshold);
                below += low ? 1 : 0;
                model.addRow(new Object[]{s.getRollNo(), s.getStudentName(), s.getSubjectCode(), s.getTotalClasses(),
                        s.getAttendedClasses(), new Percent(s.getPercentage()), low ? "Below " + threshold + "%" : "OK"});
            }
            summaryLabel.setText("   " + shown.size() + " rows, " + below + " below threshold");
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        }
    }

    private void saveThreshold() {
        try {
            attendanceService.setThreshold(user, (Integer) thresholdSpinner.getValue());
            threshold = attendanceService.getThreshold();
            UiFactory.success(this, "Attendance threshold saved: " + threshold + "%.");
            load();
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        }
    }

    private void exportCsv() {
        try {
            if (shown.isEmpty()) {
                UiFactory.warning(this, "There is nothing to export. Click Show first.");
                return;
            }
            Path file = CsvFileChooser.ask(this, "attendance_summary.csv");
            if (file != null) {
                reportService.exportAttendanceSummaries(file, shown, threshold);
                UiFactory.success(this, "Exported " + shown.size() + " rows to " + file.getFileName());
            }
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        }
    }
}
