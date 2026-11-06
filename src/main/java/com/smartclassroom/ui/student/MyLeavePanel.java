package com.smartclassroom.ui.student;

import com.smartclassroom.model.LeaveRequest;
import com.smartclassroom.model.RequestStatus;
import com.smartclassroom.model.Student;
import com.smartclassroom.service.LeaveService;
import com.smartclassroom.ui.common.CrudPanel;
import com.smartclassroom.ui.theme.UiFactory;

import javax.swing.JButton;
import java.util.List;

/** A student applies for leave, follows its status and can withdraw a request that is still pending. */
public class MyLeavePanel extends CrudPanel<LeaveRequest> {
    private static final long serialVersionUID = 1L;
    private final transient Student student;
    private final transient LeaveService service = new LeaveService();

    public MyLeavePanel(Student student) {
        super("My Leave", "Apply for leave and track the decision. Pending requests can be cancelled.",
                new String[]{"From", "To", "Days", "Reason", "Status", "Decided by", "Remark"}, true, false, false);
        this.student = student;
        JButton cancel = UiFactory.secondaryButton("Cancel request");
        cancel.addActionListener(e -> withSelection(this::cancelRequest));
        addToolbarButton(cancel);
    }

    @Override
    protected List<LeaveRequest> loadItems() {
        return service.getForStudent(student);
    }

    @Override
    protected Object[] toRow(LeaveRequest r) {
        return new Object[]{r.getFromDate(), r.getToDate(), r.getDays(), r.getReason(),
                r.getStatus().getDisplayName(), r.getReviewerName() == null ? "-" : r.getReviewerName(),
                r.getReviewRemark() == null ? "-" : r.getReviewRemark()};
    }

    @Override
    protected void onAdd() {
        if (new LeaveDialog(owner(), student).showDialog()) {
            UiFactory.success(this, "Your leave request was sent for approval.");
            refresh();
        }
    }

    private void cancelRequest(LeaveRequest r) {
        if (r.getStatus() != RequestStatus.PENDING) {
            UiFactory.warning(this, "Only a pending request can be cancelled.");
            return;
        }
        if (UiFactory.confirm(this, "Cancel your leave request for " + r.getFromDate() + " to " + r.getToDate() + "?",
                "Cancel request")) {
            service.cancel(student, r.getLeaveId());
            refresh();
        }
    }
}
