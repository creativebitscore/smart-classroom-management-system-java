package com.smartclassroom.ui.admin;

import com.smartclassroom.model.Classroom;
import com.smartclassroom.model.Faculty;
import com.smartclassroom.model.Subject;
import com.smartclassroom.model.TimetableEntry;
import com.smartclassroom.model.User;
import com.smartclassroom.service.ClassroomService;
import com.smartclassroom.service.FacultyService;
import com.smartclassroom.service.SubjectService;
import com.smartclassroom.service.TimetableService;
import com.smartclassroom.service.exception.ValidationException;
import com.smartclassroom.ui.common.FormDialog;
import com.smartclassroom.util.Validator;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Window;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/** Add or edit one timetable slot: subject + teacher + room + section + day + time. */
public class TimetableDialog extends FormDialog {
    private static final long serialVersionUID = 1L;
    private static final DayOfWeek[] DAYS = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY};

    private final transient TimetableService service = new TimetableService();
    private final transient User actor;
    private final transient TimetableEntry existing;

    private final JComboBox<Subject> subject;
    private final JComboBox<Faculty> faculty;
    private final JComboBox<Classroom> classroom;
    private final JTextField section = new JTextField(10);
    private final JComboBox<DayOfWeek> day = new JComboBox<>(DAYS);
    private final JComboBox<String> start = new JComboBox<>(timeChoices());
    private final JComboBox<String> end = new JComboBox<>(timeChoices());

    public TimetableDialog(Window owner, User actor, TimetableEntry existing) {
        super(owner, existing == null ? "Schedule a class" : "Edit scheduled class");
        this.actor = actor;
        this.existing = existing;

        subject = new JComboBox<>(new SubjectService().getAll().toArray(new Subject[0]));
        faculty = new JComboBox<>(new FacultyService().getAll(actor).toArray(new Faculty[0]));
        classroom = new JComboBox<>(new ClassroomService().getAll().toArray(new Classroom[0]));
        start.setEditable(true);
        end.setEditable(true);
        day.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                                                          boolean focus) {
                String text = value == null ? "" : value.toString().charAt(0) + value.toString().substring(1).toLowerCase();
                return super.getListCellRendererComponent(list, text, index, selected, focus);
            }
        });

        form.addField("Subject", subject);
        form.addField("Faculty", faculty);
        form.addField("Classroom", classroom);
        form.addField("Section (for example A)", section);
        form.addField("Day", day);
        form.addField("Start time (HH:mm)", start);
        form.addField("End time (HH:mm)", end);

        if (existing != null) {
            select(subject, s -> s.getSubjectId() == existing.getSubject().getSubjectId());
            select(faculty, f -> f.getFacultyId() == existing.getFaculty().getFacultyId());
            select(classroom, c -> c.getClassroomId() == existing.getClassroom().getClassroomId());
            section.setText(existing.getSection());
            day.setSelectedItem(existing.getDayOfWeek());
            start.setSelectedItem(existing.getStartTime().toString());
            end.setSelectedItem(existing.getEndTime().toString());
        } else {
            start.setSelectedItem("09:00");
            end.setSelectedItem("10:00");
        }
    }

    private static <T> void select(JComboBox<T> box, java.util.function.Predicate<T> match) {
        for (int i = 0; i < box.getItemCount(); i++) {
            if (match.test(box.getItemAt(i))) {
                box.setSelectedIndex(i);
                return;
            }
        }
    }

    /** 08:00, 08:30 ... 18:00 */
    private static String[] timeChoices() {
        String[] times = new String[21];
        for (int i = 0; i < times.length; i++) {
            times[i] = LocalTime.of(8 + i / 2, (i % 2) * 30).toString();
        }
        return times;
    }

    private static LocalTime parseTime(Object value, String fieldName) {
        try {
            return LocalTime.parse(String.valueOf(value).trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException(fieldName + " must look like 09:30 (24-hour HH:mm).");
        }
    }

    @Override
    protected void save() {
        TimetableEntry entry = existing == null ? new TimetableEntry() : existing;
        entry.setSubject(Validator.requireNotNull((Subject) subject.getSelectedItem(), "a subject"));
        entry.setFaculty(Validator.requireNotNull((Faculty) faculty.getSelectedItem(), "a faculty member"));
        entry.setClassroom(Validator.requireNotNull((Classroom) classroom.getSelectedItem(), "a classroom"));
        entry.setSection(section.getText());
        entry.setDayOfWeek((DayOfWeek) day.getSelectedItem());
        entry.setStartTime(parseTime(start.getSelectedItem(), "Start time"));
        entry.setEndTime(parseTime(end.getSelectedItem(), "End time"));
        if (existing == null) {
            service.add(actor, entry);
        } else {
            service.update(actor, entry);
        }
    }
}
