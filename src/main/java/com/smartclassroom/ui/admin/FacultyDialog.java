package com.smartclassroom.ui.admin;

import com.smartclassroom.model.Faculty;
import com.smartclassroom.model.User;
import com.smartclassroom.service.FacultyService;
import com.smartclassroom.ui.common.FormDialog;

import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.Window;

/** Add or edit a faculty member. Pass null as existing to add. */
public class FacultyDialog extends FormDialog {
    private static final long serialVersionUID = 1L;
    private final transient FacultyService service = new FacultyService();
    private final transient User actor;
    private final transient Faculty existing;

    private final JTextField fullName = form.addField("Full name", new JTextField(24));
    private final JTextField username = form.addField("Username (used to log in)", new JTextField(24));
    private final JPasswordField password = new JPasswordField(24);
    private final JTextField email = form.addField("Email", new JTextField(24));
    private final JTextField employeeCode = form.addField("Employee code", new JTextField(24));
    private final JTextField phone = form.addField("Phone (10 digits, optional)", new JTextField(24));
    private final JTextField department = form.addField("Department (for example CSE)", new JTextField(24));
    private final JTextField designation = form.addField("Designation", new JTextField(24));

    public FacultyDialog(Window owner, User actor, Faculty existing) {
        super(owner, existing == null ? "Add faculty" : "Edit faculty");
        this.actor = actor;
        this.existing = existing;
        if (existing == null) {
            form.addField("Initial password (8+ characters, letters and digits)", password);
        } else {
            fullName.setText(existing.getFullName());
            username.setText(existing.getUsername());
            username.setEnabled(false);
            email.setText(existing.getEmail());
            employeeCode.setText(existing.getEmployeeCode());
            phone.setText(existing.getPhone());
            department.setText(existing.getDepartment());
            designation.setText(existing.getDesignation());
        }
    }

    @Override
    protected void save() {
        Faculty f = existing == null ? new Faculty() : existing;
        f.setFullName(fullName.getText());
        f.setUsername(username.getText());
        f.setEmail(email.getText());
        f.setEmployeeCode(employeeCode.getText());
        f.setPhone(phone.getText());
        f.setDepartment(department.getText());
        f.setDesignation(designation.getText());
        if (existing == null) {
            service.addFaculty(actor, f, new String(password.getPassword()));
        } else {
            service.updateFaculty(actor, f);
        }
    }
}
