package com.smartclassroom.ui.faculty;

import com.smartclassroom.model.Faculty;
import com.smartclassroom.model.RequestStatus;
import com.smartclassroom.model.RoomBooking;
import com.smartclassroom.service.RoomBookingService;
import com.smartclassroom.ui.common.CrudPanel;
import com.smartclassroom.ui.theme.UiFactory;

import javax.swing.JButton;
import java.util.List;

/** A faculty member asks for a free room for an extra class or event, and follows the admin's decision. */
public class RoomBookingPanel extends CrudPanel<RoomBooking> {
    private static final long serialVersionUID = 1L;
    private final transient Faculty faculty;
    private final transient RoomBookingService service = new RoomBookingService();

    public RoomBookingPanel(Faculty faculty) {
        super("Room Booking", "Request a room for an extra class, doubt session or event. The admin approves it.",
                new String[]{"Date", "Time", "Room", "Purpose", "People", "Status", "Remark"}, true, false, false);
        this.faculty = faculty;
        JButton cancel = UiFactory.secondaryButton("Cancel booking");
        cancel.addActionListener(e -> withSelection(this::cancelBooking));
        addToolbarButton(cancel);
    }

    @Override
    protected List<RoomBooking> loadItems() {
        return service.getForFaculty(faculty);
    }

    @Override
    protected Object[] toRow(RoomBooking b) {
        return new Object[]{b.getBookingDate(), b.getSlotLabel(), b.getRoomName(), b.getPurpose(),
                b.getExpectedAttendees(), b.getStatus().getDisplayName(),
                b.getReviewRemark() == null ? "-" : b.getReviewRemark()};
    }

    @Override
    protected void onAdd() {
        if (new RoomBookingDialog(owner(), faculty).showDialog()) {
            UiFactory.success(this, "Your request was sent to the admin for approval.");
            refresh();
        }
    }

    private void cancelBooking(RoomBooking b) {
        if (b.getStatus() != RequestStatus.PENDING && b.getStatus() != RequestStatus.APPROVED) {
            UiFactory.warning(this, "This booking is already " + b.getStatus().getDisplayName().toLowerCase() + ".");
            return;
        }
        if (UiFactory.confirm(this, "Cancel the booking of " + b.getRoomName() + " on " + b.getBookingDate() + "?",
                "Cancel booking")) {
            service.cancel(faculty, b.getBookingId());
            refresh();
        }
    }
}
