package com.smartclassroom.ui.admin;

import com.smartclassroom.model.Faculty;
import com.smartclassroom.model.User;
import com.smartclassroom.service.FacultyService;
import com.smartclassroom.ui.common.CrudPanel;
import com.smartclassroom.ui.common.PasswordDialog;
import com.smartclassroom.ui.theme.UiFactory;

import javax.swing.JButton;
import java.util.List;

/** Admin screen for faculty details. */
public class FacultyPanel extends CrudPanel<Faculty> {
    private static final long serialVersionUID = 1L;
    private final transient User actor;
    private final transient FacultyService service = new FacultyService();

    public FacultyPanel(User actor) {
        super("Faculty", "Manage teaching staff",
                new String[]{"Emp. Code", "Name", "Email", "Phone", "Department", "Designation", "Username"},
                true, true, true);
        this.actor = actor;
        JButton reset = UiFactory.secondaryButton("Reset Password");
        reset.addActionListener(e -> withSelection(f ->
                PasswordDialog.reset(owner(), actor, f.getUserId(), f.getFullName())));
        addToolbarButton(reset);
    }

    @Override
    protected List<Faculty> loadItems() {
        return service.getAll(actor);
    }

    @Override
    protected Object[] toRow(Faculty f) {
        return new Object[]{f.getEmployeeCode(), f.getFullName(), f.getEmail(), f.getPhone() == null ? "" : f.getPhone(),
                f.getDepartment(), f.getDesignation(), f.getUsername()};
    }

    @Override
    protected void onAdd() {
        if (new FacultyDialog(owner(), actor, null).showDialog()) {
            UiFactory.success(this, "Faculty member added successfully.");
            refresh();
        }
    }

    @Override
    protected void onEdit(Faculty f) {
        if (new FacultyDialog(owner(), actor, f).showDialog()) {
            UiFactory.success(this, "Faculty details updated successfully.");
            refresh();
        }
    }

    @Override
    protected void onDelete(Faculty f) {
        service.deleteFaculty(actor, f.getFacultyId());
    }

    @Override
    protected String deleteMessage(Faculty f) {
        return "Delete " + f.getFullName() + "?\n\nTheir login, timetable entries and assignments will also be removed.";
    }
}
