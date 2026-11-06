package com.smartclassroom.ui.admin;

import com.smartclassroom.model.Student;
import com.smartclassroom.model.User;
import com.smartclassroom.service.AuthService;
import com.smartclassroom.service.ReportService;
import com.smartclassroom.service.StudentService;
import com.smartclassroom.ui.common.CrudPanel;
import com.smartclassroom.ui.common.CsvFileChooser;
import com.smartclassroom.ui.common.PasswordDialog;
import com.smartclassroom.ui.theme.UiFactory;

import javax.swing.JButton;
import java.nio.file.Path;
import java.util.List;

/** Student records. Admin can add, edit and delete; faculty get a read-only list with CSV export. */
public class StudentPanel extends CrudPanel<Student> {
    private static final long serialVersionUID = 1L;
    private final transient User actor;
    private final transient StudentService service = new StudentService();
    private final transient ReportService reportService = new ReportService();

    public StudentPanel(User actor, boolean readOnly) {
        super("Students", readOnly ? "All students (read only)" : "Add, update and remove student records",
                new String[]{"Roll No", "Name", "Email", "Phone", "Branch", "Sem", "Section", "Username"},
                !readOnly, !readOnly, !readOnly);
        this.actor = actor;

        JButton export = UiFactory.secondaryButton("Export CSV");
        export.addActionListener(e -> guarded(this::exportCsv));
        addToolbarButton(export);
        if (!readOnly) {
            JButton reset = UiFactory.secondaryButton("Reset Password");
            reset.addActionListener(e -> withSelection(s ->
                    PasswordDialog.reset(owner(), actor, s.getUserId(), s.getFullName())));
            addToolbarButton(reset);
        }
    }

    @Override
    protected List<Student> loadItems() {
        return service.getAll(actor);
    }

    @Override
    protected Object[] toRow(Student s) {
        return new Object[]{s.getRollNo(), s.getFullName(), s.getEmail(), s.getPhone() == null ? "" : s.getPhone(),
                s.getBranch(), s.getSemester(), s.getSection(), s.getUsername()};
    }

    @Override
    protected void onAdd() {
        if (new StudentDialog(owner(), actor, null).showDialog()) {
            UiFactory.success(this, "Student added successfully.");
            refresh();
        }
    }

    @Override
    protected void onEdit(Student s) {
        if (new StudentDialog(owner(), actor, s).showDialog()) {
            UiFactory.success(this, "Student updated successfully.");
            refresh();
        }
    }

    @Override
    protected void onDelete(Student s) {
        service.deleteStudent(actor, s.getStudentId());
    }

    @Override
    protected String deleteMessage(Student s) {
        return "Delete " + s.getFullName() + " (" + s.getRollNo() + ")?\n\nTheir login, attendance and marks "
                + "will also be removed. This cannot be undone.";
    }

    private void exportCsv() {
        Path file = CsvFileChooser.ask(this, "students.csv");
        if (file != null) {
            reportService.exportStudents(actor, file, currentItems());
            UiFactory.success(this, "Exported " + currentItems().size() + " students to " + file.getFileName());
        }
    }
}
