package com.smartclassroom.ui.admin;

import com.smartclassroom.model.Subject;
import com.smartclassroom.model.User;
import com.smartclassroom.service.SubjectService;
import com.smartclassroom.ui.common.FormDialog;
import com.smartclassroom.util.Validator;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.awt.Window;

/** Add or edit a subject. */
public class SubjectDialog extends FormDialog {
    private static final long serialVersionUID = 1L;
    private final transient SubjectService service = new SubjectService();
    private final transient User actor;
    private final transient Subject existing;

    private final JTextField code = form.addField("Subject code (for example CS301)", new JTextField(24));
    private final JTextField name = form.addField("Subject name", new JTextField(24));
    private final JTextField branch = form.addField("Branch (for example CSE)", new JTextField(24));
    private final JComboBox<Integer> semester = form.addField("Semester",
            new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6, 7, 8}));
    private final JComboBox<Integer> credits = form.addField("Credits",
            new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6}));

    public SubjectDialog(Window owner, User actor, Subject existing) {
        super(owner, existing == null ? "Add subject" : "Edit subject");
        this.actor = actor;
        this.existing = existing;
        credits.setSelectedItem(3);
        if (existing != null) {
            code.setText(existing.getSubjectCode());
            name.setText(existing.getSubjectName());
            branch.setText(existing.getBranch());
            semester.setSelectedItem(existing.getSemester());
            credits.setSelectedItem(existing.getCredits());
        }
    }

    @Override
    protected void save() {
        Subject s = existing == null ? new Subject() : existing;
        s.setSubjectCode(code.getText());
        s.setSubjectName(name.getText());
        s.setBranch(branch.getText());
        s.setSemester((Integer) Validator.requireNotNull(semester.getSelectedItem(), "a semester"));
        s.setCredits((Integer) Validator.requireNotNull(credits.getSelectedItem(), "credits"));
        if (existing == null) {
            service.add(actor, s);
        } else {
            service.update(actor, s);
        }
    }
}
