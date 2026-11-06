package com.smartclassroom.ui.faculty;

import com.smartclassroom.model.Faculty;
import com.smartclassroom.model.Mark;
import com.smartclassroom.model.Student;
import com.smartclassroom.model.Subject;
import com.smartclassroom.service.AttendanceService;
import com.smartclassroom.service.MarkService;
import com.smartclassroom.service.SubjectService;
import com.smartclassroom.service.exception.ValidationException;
import com.smartclassroom.ui.common.AppTableModel;
import com.smartclassroom.ui.common.Refreshable;
import com.smartclassroom.ui.theme.RoundedPanel;
import com.smartclassroom.ui.theme.Theme;
import com.smartclassroom.ui.theme.UiFactory;
import com.smartclassroom.util.Validator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Enter or update the marks of a whole class for one exam. */
public class MarksPanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;
    private static final String[] EXAMS = {"MID1", "MID2", "ASSIGNMENT", "QUIZ", "FINAL"};

    private final transient Faculty faculty;
    private final transient MarkService markService = new MarkService();
    private final transient AttendanceService attendanceService = new AttendanceService();
    private final transient SubjectService subjectService = new SubjectService();

    private final JComboBox<Subject> subjectBox = new JComboBox<>();
    private final JComboBox<String> sectionBox = new JComboBox<>();
    private final JComboBox<String> examBox = new JComboBox<>(EXAMS);
    private final JTextField maxField = new JTextField("25", 5);
    private final AppTableModel model = new AppTableModel(new String[]{"Roll No", "Student", "Marks obtained"}, 2);
    private final JTable table = UiFactory.table(model);
    private final JLabel infoLabel = UiFactory.muted("Choose subject, section and exam, then click Load class.");
    private transient List<Student> roster = new ArrayList<>();
    private boolean loadingSections;

    public MarksPanel(Faculty faculty) {
        super(new BorderLayout(0, 16));
        this.faculty = faculty;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        examBox.setEditable(true);

        JPanel titles = new JPanel(new BorderLayout());
        titles.setOpaque(false);
        titles.add(UiFactory.title("Marks"), BorderLayout.NORTH);
        titles.add(UiFactory.muted("Enter marks for a class; saving again updates the same exam"), BorderLayout.SOUTH);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        controls.setOpaque(false);
        controls.add(UiFactory.muted("Subject"));
        controls.add(subjectBox);
        controls.add(UiFactory.muted("Section"));
        controls.add(sectionBox);
        controls.add(UiFactory.muted("Exam"));
        controls.add(examBox);
        controls.add(UiFactory.muted("Out of"));
        controls.add(maxField);
        JButton load = UiFactory.primaryButton("Load class");
        load.addActionListener(e -> loadClass());
        controls.add(load);

        JPanel north = new JPanel(new BorderLayout(0, 6));
        north.setOpaque(false);
        north.add(titles, BorderLayout.NORTH);
        north.add(controls, BorderLayout.SOUTH);

        table.setAutoCreateRowSorter(false);
        table.setRowSorter(null);
        JButton save = UiFactory.primaryButton("Save marks");
        save.addActionListener(e -> save());
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(infoLabel, BorderLayout.WEST);
        bottom.add(save, BorderLayout.EAST);

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
            String exam = String.valueOf(examBox.getSelectedItem());
            roster = markService.getRoster(faculty, subject.getSubjectId(), section);
            Map<Integer, Mark> existing = markService.getExisting(faculty, subject.getSubjectId(), exam);
            model.setRowCount(0);
            for (Student s : roster) {
                Mark m = existing.get(s.getStudentId());
                model.addRow(new Object[]{s.getRollNo(), s.getFullName(), m == null ? "" : format(m.getMarksObtained())});
                if (m != null) {
                    maxField.setText(format(m.getMaxMarks()));
                }
            }
            infoLabel.setText(roster.size() + " students" + (existing.isEmpty() ? "" : ", existing marks loaded"));
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        }
    }

    private static String format(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
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
            double max = Validator.parseDouble(maxField.getText(), "Out of");
            Map<Integer, Double> marks = new LinkedHashMap<>();
            for (int row = 0; row < roster.size(); row++) {
                String text = String.valueOf(model.getValueAt(row, 2)).trim();
                if (text.isEmpty()) {
                    continue; // blank = not entered yet
                }
                try {
                    marks.put(roster.get(row).getStudentId(), Validator.parseDouble(text, "Marks"));
                } catch (ValidationException e) {
                    throw new ValidationException("Marks for " + roster.get(row).getRollNo() + " must be a number.");
                }
            }
            markService.saveMarks(faculty, subject.getSubjectId(), String.valueOf(examBox.getSelectedItem()), max, marks);
            UiFactory.success(this, "Marks saved for " + marks.size() + " students.");
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        }
    }
}
