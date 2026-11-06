package com.smartclassroom.ui.admin;

import com.smartclassroom.model.Subject;
import com.smartclassroom.model.User;
import com.smartclassroom.service.SubjectService;
import com.smartclassroom.ui.common.CrudPanel;
import com.smartclassroom.ui.theme.UiFactory;

import java.util.List;

/** Admin screen for subjects. */
public class SubjectPanel extends CrudPanel<Subject> {
    private static final long serialVersionUID = 1L;
    private final transient User actor;
    private final transient SubjectService service = new SubjectService();

    public SubjectPanel(User actor) {
        super("Subjects", "Courses offered to each branch and semester",
                new String[]{"Code", "Name", "Branch", "Semester", "Credits"}, true, true, true);
        this.actor = actor;
    }

    @Override
    protected List<Subject> loadItems() {
        return service.getAll();
    }

    @Override
    protected Object[] toRow(Subject s) {
        return new Object[]{s.getSubjectCode(), s.getSubjectName(), s.getBranch(), s.getSemester(), s.getCredits()};
    }

    @Override
    protected void onAdd() {
        if (new SubjectDialog(owner(), actor, null).showDialog()) {
            UiFactory.success(this, "Subject added successfully.");
            refresh();
        }
    }

    @Override
    protected void onEdit(Subject s) {
        if (new SubjectDialog(owner(), actor, s).showDialog()) {
            UiFactory.success(this, "Subject updated successfully.");
            refresh();
        }
    }

    @Override
    protected void onDelete(Subject s) {
        service.delete(actor, s.getSubjectId());
    }

    @Override
    protected String deleteMessage(Subject s) {
        return "Delete " + s + "?\n\nIts timetable entries, attendance, assignments and marks will also be removed.";
    }
}
