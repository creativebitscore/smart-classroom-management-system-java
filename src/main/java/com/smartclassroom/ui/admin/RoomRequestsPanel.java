package com.smartclassroom.ui.admin;

import com.smartclassroom.model.RequestStatus;
import com.smartclassroom.model.RoomBooking;
import com.smartclassroom.model.User;
import com.smartclassroom.service.RoomBookingService;
import com.smartclassroom.ui.common.CrudPanel;
import com.smartclassroom.ui.theme.UiFactory;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import java.util.List;

/** The admin approves or rejects room requests. Approval re-checks the room, so double booking is impossible. */
public class RoomRequestsPanel extends CrudPanel<RoomBooking> {
    private static final long serialVersionUID = 1L;
    private final transient User admin;
    private final transient RoomBookingService service = new RoomBookingService();

    public RoomRequestsPanel(User admin) {
        super("Room Requests", "Extra classes and events requested by faculty. Pending requests are shown first.",
                new String[]{"Date", "Time", "Room", "Faculty", "Purpose", "People", "Status", "Remark"},
                false, false, false);
        this.admin = admin;
        JButton reject = UiFactory.dangerButton("Reject");
        reject.addActionListener(e -> withSelection(this::reject));
        JButton approve = UiFactory.primaryButton("Approve");
        approve.addActionListener(e -> withSelection(this::approve));
        addToolbarButton(reject);
        addToolbarButton(approve);
    }

    @Override
    protected List<RoomBooking> loadItems() {
        return service.getAll(admin);
    }

    @Override
    protected Object[] toRow(RoomBooking b) {
        return new Object[]{b.getBookingDate(), b.getSlotLabel(), b.getRoomName(), b.getFacultyName(),
                b.getPurpose(), b.getExpectedAttendees(), b.getStatus().getDisplayName(),
                b.getReviewRemark() == null ? "-" : b.getReviewRemark()};
    }

    private void approve(RoomBooking b) {
        if (!isPending(b)) {
            return;
        }
        String remark = JOptionPane.showInputDialog(this, "Remark for " + b.getFacultyName() + " (optional):",
                "Approve booking", JOptionPane.PLAIN_MESSAGE);
        if (remark != null) {
            service.approve(admin, b.getBookingId(), remark);
            refresh();
        }
    }

    private void reject(RoomBooking b) {
        if (!isPending(b)) {
            return;
        }
        String reason = JOptionPane.showInputDialog(this, "Why is this request rejected?", "Reject booking",
                JOptionPane.PLAIN_MESSAGE);
        if (reason != null) {
            service.reject(admin, b.getBookingId(), reason);
            refresh();
        }
    }

    private boolean isPending(RoomBooking b) {
        if (b.getStatus() != RequestStatus.PENDING) {
            UiFactory.warning(this, "This request is already " + b.getStatus().getDisplayName().toLowerCase() + ".");
            return false;
        }
        return true;
    }
}
