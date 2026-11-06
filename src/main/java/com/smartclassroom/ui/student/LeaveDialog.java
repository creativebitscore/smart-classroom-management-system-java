package com.smartclassroom.ui.student;

import com.smartclassroom.model.LeaveRequest;
import com.smartclassroom.model.Student;
import com.smartclassroom.service.LeaveService;
import com.smartclassroom.ui.common.DateField;
import com.smartclassroom.ui.common.FormDialog;
import com.smartclassroom.ui.theme.UiFactory;

import javax.swing.JTextArea;
import java.awt.Window;
import java.time.LocalDate;

/** Form for a new leave request. */
public class LeaveDialog extends FormDialog {
    private static final long serialVersionUID = 1L;
    private final transient LeaveService service = new LeaveService();
    private final transient Student student;

    private final DateField from = new DateField(LocalDate.now());
    private final DateField to = new DateField(LocalDate.now());
    private final JTextArea reason = new JTextArea(5, 30);

    public LeaveDialog(Window owner, Student student) {
        super(owner, "Apply for leave");
        this.student = student;
        form.addField("First day (yyyy-MM-dd)", from);
        form.addField("Last day (yyyy-MM-dd)", to);
        reason.setLineWrap(true);
        reason.setWrapStyleWord(true);
        form.addField("Reason", UiFactory.scroll(reason));
    }

    @Override
    protected void save() {
        LeaveRequest request = new LeaveRequest();
        request.setFromDate(from.getDate("First day"));
        request.setToDate(to.getDate("Last day"));
        request.setReason(reason.getText());
        service.apply(student, request);
    }
}
