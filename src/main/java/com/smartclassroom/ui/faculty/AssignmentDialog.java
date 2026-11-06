package com.smartclassroom.ui.faculty;

import com.smartclassroom.model.Assignment;
import com.smartclassroom.model.Faculty;
import com.smartclassroom.model.Subject;
import com.smartclassroom.service.AssignmentService;
import com.smartclassroom.service.SubjectService;
import com.smartclassroom.ui.common.DateField;
import com.smartclassroom.ui.common.FormDialog;
import com.smartclassroom.ui.theme.UiFactory;
import com.smartclassroom.util.Validator;

import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Window;
import java.time.LocalDate;

/** Create or edit an assignment. */
public class AssignmentDialog extends FormDialog {
    private static final long serialVersionUID = 1L;
    private final transient AssignmentService service = new AssignmentService();
    private final transient Faculty faculty;
    private final transient Assignment existing;

    private final JComboBox<Subject> subject;
    private final JTextField title = new JTextField(30);
    private final JTextArea description = new JTextArea(5, 30);
    private final DateField dueDate = new DateField(LocalDate.now().plusDays(7));

    public AssignmentDialog(Window owner, Faculty faculty, Assignment existing) {
        super(owner, existing == null ? "New assignment" : "Edit assignment");
        this.faculty = faculty;
        this.existing = existing;
        subject = new JComboBox<>(new SubjectService().getTaughtBy(faculty).toArray(new Subject[0]));
        form.addField("Subject", subject);
        form.addField("Title", title);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        form.addField("Description (optional)", UiFactory.scroll(description));
        form.addField("Due date (yyyy-MM-dd)", dueDate);
        if (existing != null) {
            for (int i = 0; i < subject.getItemCount(); i++) {
                if (subject.getItemAt(i).getSubjectId() == existing.getSubjectId()) {
                    subject.setSelectedIndex(i);
                }
            }
            title.setText(existing.getTitle());
            description.setText(existing.getDescription());
            dueDate.setDate(existing.getDueDate());
        }
    }

    @Override
    protected void save() {
        Subject chosen = Validator.requireNotNull((Subject) subject.getSelectedItem(), "a subject");
        Assignment a = existing == null ? new Assignment() : existing;
        a.setSubjectId(chosen.getSubjectId());
        a.setTitle(title.getText());
        a.setDescription(description.getText().trim().isEmpty() ? null : description.getText().trim());
        a.setDueDate(dueDate.getDate("Due date"));
        if (existing == null) {
            service.create(faculty, a);
        } else {
            service.update(faculty, a);
        }
    }
}
