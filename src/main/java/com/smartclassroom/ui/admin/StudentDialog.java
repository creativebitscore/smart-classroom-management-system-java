package com.smartclassroom.ui.admin;

import com.smartclassroom.model.Student;
import com.smartclassroom.model.User;
import com.smartclassroom.service.StudentService;
import com.smartclassroom.ui.common.FormDialog;
import com.smartclassroom.util.Validator;

import javax.swing.JComboBox;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.Window;

/** Add or edit a student. Pass null as existing to add. */
public class StudentDialog extends FormDialog {
    private static final long serialVersionUID = 1L;
    private final transient StudentService service = new StudentService();
    private final transient User actor;
    private final transient Student existing;

    private final JTextField fullName = form.addField("Full name", new JTextField(24));
    private final JTextField username = form.addField("Username (used to log in)", new JTextField(24));
    private final JPasswordField password = new JPasswordField(24);
    private final JTextField email = form.addField("Email", new JTextField(24));
    private final JTextField rollNo = form.addField("Roll number", new JTextField(24));
    private final JTextField phone = form.addField("Phone (10 digits, optional)", new JTextField(24));
    private final JTextField branch = form.addField("Branch (for example CSE)", new JTextField(24));
    private final JComboBox<Integer> semester = form.addField("Semester",
            new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6, 7, 8}));
    private final JTextField section = form.addField("Section (for example A)", new JTextField(24));

    public StudentDialog(Window owner, User actor, Student existing) {
        super(owner, existing == null ? "Add student" : "Edit student");
        this.actor = actor;
        this.existing = existing;
        if (existing == null) {
            form.addField("Initial password (8+ characters, letters and digits)", password);
        } else {
            fullName.setText(existing.getFullName());
            username.setText(existing.getUsername());
            username.setEnabled(false); // the login name never changes
            email.setText(existing.getEmail());
            rollNo.setText(existing.getRollNo());
            phone.setText(existing.getPhone());
            branch.setText(existing.getBranch());
            semester.setSelectedItem(existing.getSemester());
            section.setText(existing.getSection());
        }
    }

    @Override
    protected void save() {
        Student s = existing == null ? new Student() : existing;
        s.setFullName(fullName.getText());
        s.setUsername(username.getText());
        s.setEmail(email.getText());
        s.setRollNo(rollNo.getText());
        s.setPhone(phone.getText());
        s.setBranch(branch.getText());
        s.setSemester((Integer) Validator.requireNotNull(semester.getSelectedItem(), "a semester"));
        s.setSection(section.getText());
        if (existing == null) {
            service.addStudent(actor, s, new String(password.getPassword()));
        } else {
            service.updateStudent(actor, s);
        }
    }
}
