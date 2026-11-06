package com.smartclassroom.ui.common;

import com.smartclassroom.model.LeaveRequest;
import com.smartclassroom.model.RequestStatus;
import com.smartclassroom.model.User;
import com.smartclassroom.service.LeaveService;
import com.smartclassroom.ui.theme.UiFactory;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import java.util.List;

/** Admin and faculty review student leave requests. Pending ones are listed first; type "Pending" in the search box to filter. */
public class LeaveReviewPanel extends CrudPanel<LeaveRequest> {
    private static final long serialVersionUID = 1L;
    private final transient User reviewer;
    private final transient LeaveService service = new LeaveService();

    public LeaveReviewPanel(User reviewer) {
        super("Leave Requests", "Approve or reject student leave. Pending requests are shown first.",
                new String[]{"Student", "Roll no", "From", "To", "Days", "Reason", "Status", "Decided by", "Remark"},
                false, false, false);
        this.reviewer = reviewer;
        JButton reject = UiFactory.dangerButton("Reject");
        reject.addActionListener(e -> withSelection(this::reject));
        JButton approve = UiFactory.primaryButton("Approve");
        approve.addActionListener(e -> withSelection(this::approve));
        addToolbarButton(reject);
        addToolbarButton(approve);
    }

    @Override
    protected List<LeaveRequest> loadItems() {
        return service.getForReview(reviewer, null);
    }

    @Override
    protected Object[] toRow(LeaveRequest r) {
        return new Object[]{r.getStudentName(), r.getRollNo(), r.getFromDate(), r.getToDate(), r.getDays(),
                r.getReason(), r.getStatus().getDisplayName(),
                r.getReviewerName() == null ? "-" : r.getReviewerName(),
                r.getReviewRemark() == null ? "-" : r.getReviewRemark()};
    }

    private void approve(LeaveRequest r) {
        if (!isPending(r)) {
            return;
        }
        String remark = JOptionPane.showInputDialog(this, "Remark for " + r.getStudentName() + " (optional):",
                "Approve leave", JOptionPane.PLAIN_MESSAGE);
        if (remark != null) {
            service.approve(reviewer, r.getLeaveId(), remark);
            refresh();
        }
    }

    private void reject(LeaveRequest r) {
        if (!isPending(r)) {
            return;
        }
        String reason = JOptionPane.showInputDialog(this, "Why is the leave of " + r.getStudentName() + " rejected?",
                "Reject leave", JOptionPane.PLAIN_MESSAGE);
        if (reason != null) {
            service.reject(reviewer, r.getLeaveId(), reason);
            refresh();
        }
    }

    private boolean isPending(LeaveRequest r) {
        if (r.getStatus() != RequestStatus.PENDING) {
            UiFactory.warning(this, "This request is already " + r.getStatus().getDisplayName().toLowerCase() + ".");
            return false;
        }
        return true;
    }
}
