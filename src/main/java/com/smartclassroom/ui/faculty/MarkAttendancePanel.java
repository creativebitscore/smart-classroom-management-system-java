package com.smartclassroom.ui.faculty;

import com.smartclassroom.model.AttendanceRecord;
import com.smartclassroom.model.AttendanceStatus;
import com.smartclassroom.model.Faculty;
import com.smartclassroom.model.Student;
import com.smartclassroom.model.Subject;
import com.smartclassroom.service.AttendanceService;
import com.smartclassroom.service.SubjectService;
import com.smartclassroom.service.exception.ValidationException;
import com.smartclassroom.ui.common.AppTableModel;
import com.smartclassroom.ui.common.DateField;
import com.smartclassroom.ui.common.Refreshable;
import com.smartclassroom.ui.theme.RoundedPanel;
import com.smartclassroom.ui.theme.Theme;
import com.smartclassroom.ui.theme.UiFactory;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mark attendance for one class on one date. If the date was already marked, the saved statuses are
 * shown and the button changes to "Update attendance", so duplicates can never be created.
 */
public class MarkAttendancePanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;

    private final transient Faculty faculty;
    private final transient AttendanceService attendanceService = new AttendanceService();
    private final transient SubjectService subjectService = new SubjectService();

    private final JComboBox<Subject> subjectBox = new JComboBox<>();
    private final JComboBox<String> sectionBox = new JComboBox<>();
    private final DateField dateField = new DateField(LocalDate.now());
    private final AppTableModel model = new AppTableModel(new String[]{"Roll No", "Student", "Status"}, 2);
    private final JTable table = UiFactory.table(model);
    private final JLabel infoLabel = UiFactory.muted("Choose a subject, section and date, then click Load class.");
    private final JButton saveButton = UiFactory.primaryButton("Save attendance");
    private transient List<Student> roster = new ArrayList<>();
    private transient Map<Integer, AttendanceStatus> alreadySaved = new HashMap<>();
    private boolean loadingSections;

    public MarkAttendancePanel(Faculty faculty) {
        super(new BorderLayout(0, 16));
        this.faculty = faculty;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel titles = new JPanel(new BorderLayout());
        titles.setOpaque(false);
        titles.add(UiFactory.title("Mark Attendance"), BorderLayout.NORTH);
        titles.add(UiFactory.muted("Present, Absent or Late for each student"), BorderLayout.SOUTH);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        controls.setOpaque(false);
        controls.add(UiFactory.muted("Subject"));
        controls.add(subjectBox);
        controls.add(UiFactory.muted("Section"));
        controls.add(sectionBox);
        controls.add(UiFactory.muted("Date"));
        controls.add(dateField);
        JButton load = UiFactory.primaryButton("Load class");
        load.addActionListener(e -> loadClass());
        controls.add(load);

        JPanel north = new JPanel(new BorderLayout(0, 6));
        north.setOpaque(false);
        north.add(titles, BorderLayout.NORTH);
        north.add(controls, BorderLayout.SOUTH);

        table.setAutoCreateRowSorter(false);
        table.setRowSorter(null);
        JComboBox<AttendanceStatus> statusEditor = new JComboBox<>(AttendanceStatus.values());
        table.setDefaultEditor(AttendanceStatus.class, new DefaultCellEditor(statusEditor));

        JButton allPresent = UiFactory.secondaryButton("Mark all present");
        allPresent.addActionListener(e -> markAllPresent());
        saveButton.addActionListener(e -> save());
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(infoLabel, BorderLayout.WEST);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(allPresent);
        buttons.add(saveButton);
        bottom.add(buttons, BorderLayout.EAST);

        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        card.add(UiFactory.scroll(table), BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);

        add(north, BorderLayout.NORTH);
        add(card, BorderLayout.CENTER);

        subjectBox.addActionListener(e -> loadSections());
    }

    @Override
    public void refresh() {
        try {
            subjectBox.removeAllItems();
            subjectService.getTaughtBy(faculty).forEach(subjectBox::addItem);
            if (subjectBox.getItemCount() == 0) {
                infoLabel.setText("You are not assigned to any subject yet. Ask the admin to add you to the timetable.");
            }
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        }
    }

    private void loadSections() {
        Subject subject = (Subject) subjectBox.getSelectedItem();
        sectionBox.removeAllItems();
        if (subject == null) {
            return;
        }
        try {
            loadingSections = true;
            attendanceService.getSectionsTaught(faculty, subject.getSubjectId()).forEach(sectionBox::addItem);
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        } finally {
            loadingSections = false;
        }
    }

    private void loadClass() {
        try {
            if (loadingSections) {
                return;
            }
            Subject subject = (Subject) subjectBox.getSelectedItem();
            String section = (String) sectionBox.getSelectedItem();
            if (subject == null || section == null) {
                throw new ValidationException("Please select a subject and a section.");
            }
            LocalDate date = dateField.getDate("Date");
            roster = attendanceService.getRoster(faculty, subject.getSubjectId(), section);
            alreadySaved = new HashMap<>();
            for (AttendanceRecord r : attendanceService.getExisting(faculty, subject.getSubjectId(), date, section)) {
                alreadySaved.put(r.getStudentId(), r.getStatus());
            }
            model.setRowCount(0);
            for (Student s : roster) {
                model.addRow(new Object[]{s.getRollNo(), s.getFullName(),
                        alreadySaved.getOrDefault(s.getStudentId(), AttendanceStatus.PRESENT)});
            }
            if (roster.isEmpty()) {
                infoLabel.setText("No students found in this class.");
            } else if (alreadySaved.isEmpty()) {
                infoLabel.setText(roster.size() + " students. Attendance for " + date + " is not saved yet.");
            } else {
                infoLabel.setText(roster.size() + " students. Attendance for " + date
                        + " is already saved. Changing a status and saving will update it.");
            }
            saveButton.setText(alreadySaved.isEmpty() ? "Save attendance" : "Update attendance");
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        }
    }

    private void markAllPresent() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        for (int row = 0; row < model.getRowCount(); row++) {
            model.setValueAt(AttendanceStatus.PRESENT, row, 2);
        }
    }

    private void save() {
        try {
            if (table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }
            if (roster.isEmpty()) {
                throw new ValidationException("Load a class with students first.");
            }
            Subject subject = (Subject) subjectBox.getSelectedItem();
            LocalDate date = dateField.getDate("Date");

            // students already saved are updated; students not yet saved for this date are added
            Map<Integer, AttendanceStatus> toUpdate = new LinkedHashMap<>();
            Map<Integer, AttendanceStatus> toAdd = new LinkedHashMap<>();
            for (int row = 0; row < roster.size(); row++) {
                int studentId = roster.get(row).getStudentId();
                AttendanceStatus status = (AttendanceStatus) model.getValueAt(row, 2);
                (alreadySaved.containsKey(studentId) ? toUpdate : toAdd).put(studentId, status);
            }
            if (!toUpdate.isEmpty()) {
                attendanceService.updateAttendance(faculty, subject.getSubjectId(), date, toUpdate);
            }
            if (!toAdd.isEmpty()) {
                attendanceService.markAttendance(faculty, subject.getSubjectId(), date, toAdd);
            }
            UiFactory.success(this, "Attendance saved for " + roster.size() + " students on " + date + ".");
            loadClass();
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        }
    }
}
