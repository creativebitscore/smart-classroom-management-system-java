package com.smartclassroom.ui.faculty;

import com.smartclassroom.model.Assignment;
import com.smartclassroom.model.Faculty;
import com.smartclassroom.service.AssignmentService;
import com.smartclassroom.ui.common.CrudPanel;
import com.smartclassroom.ui.theme.UiFactory;

import java.time.LocalDate;
import java.util.List;

/** Faculty create, edit and delete their assignments. */
public class AssignmentPanel extends CrudPanel<Assignment> {
    private static final long serialVersionUID = 1L;
    private final transient Faculty faculty;
    private final transient AssignmentService service = new AssignmentService();

    public AssignmentPanel(Faculty faculty) {
        super("Assignments", "Publish work with a deadline; students see it immediately",
                new String[]{"Subject", "Title", "Due date", "Status"}, true, true, true);
        this.faculty = faculty;
    }

    @Override
    protected List<Assignment> loadItems() {
        return service.getForFaculty(faculty);
    }

    @Override
    protected Object[] toRow(Assignment a) {
        return new Object[]{a.getSubjectName(), a.getTitle(), a.getDueDate(),
                a.isOverdue(LocalDate.now()) ? "Closed" : "Open"};
    }

    @Override
    protected void onAdd() {
        if (new AssignmentDialog(owner(), faculty, null).showDialog()) {
            UiFactory.success(this, "Assignment published.");
            refresh();
        }
    }

    @Override
    protected void onEdit(Assignment a) {
        if (new AssignmentDialog(owner(), faculty, a).showDialog()) {
            UiFactory.success(this, "Assignment updated.");
            refresh();
        }
    }

    @Override
    protected void onDelete(Assignment a) {
        service.delete(faculty, a.getAssignmentId());
    }

    @Override
    protected String deleteMessage(Assignment a) {
        return "Delete the assignment \"" + a.getTitle() + "\"? Students will no longer see it.";
    }
}
