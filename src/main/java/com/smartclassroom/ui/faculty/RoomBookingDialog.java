package com.smartclassroom.ui.faculty;

import com.smartclassroom.model.Classroom;
import com.smartclassroom.model.Faculty;
import com.smartclassroom.model.RoomBooking;
import com.smartclassroom.service.RoomBookingService;
import com.smartclassroom.service.exception.AppException;
import com.smartclassroom.ui.common.DateField;
import com.smartclassroom.ui.common.FormDialog;
import com.smartclassroom.ui.theme.UiFactory;
import com.smartclassroom.util.Validator;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.awt.Window;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Request a room. Fill in the date, time and number of people, press "Find free rooms" and the room list
 * shows only rooms that are big enough and free (no regular class, no other approved booking).
 */
public class RoomBookingDialog extends FormDialog {
    private static final long serialVersionUID = 1L;
    private final transient RoomBookingService service = new RoomBookingService();
    private final transient Faculty faculty;

    private final DateField date = new DateField(LocalDate.now().plusDays(1));
    private final JComboBox<String> start = new JComboBox<>(timeChoices());
    private final JComboBox<String> end = new JComboBox<>(timeChoices());
    private final JTextField attendees = new JTextField("30", 6);
    private final JTextField purpose = new JTextField(30);
    private final JComboBox<Classroom> room = new JComboBox<>();

    public RoomBookingDialog(Window owner, Faculty faculty) {
        super(owner, "Request a room");
        this.faculty = faculty;
        start.setSelectedItem("14:00");
        end.setSelectedItem("15:00");
        form.addField("Purpose", purpose);
        form.addField("Date (yyyy-MM-dd)", date);
        form.addField("Start time (HH:mm)", start);
        form.addField("End time (HH:mm)", end);
        form.addField("Expected number of people", attendees);

        JButton find = UiFactory.secondaryButton("Find free rooms");
        find.addActionListener(e -> findRooms());
        form.addField("Availability", find);
        form.addField("Room", room);
    }

    private void findRooms() {
        try {
            List<Classroom> free = service.findAvailableRooms(faculty, date.getDate("Date"),
                    parseTime(start.getSelectedItem(), "Start time"), parseTime(end.getSelectedItem(), "End time"),
                    Validator.parseInt(attendees.getText(), "Expected number of people"));
            room.removeAllItems();
            free.forEach(room::addItem);
            if (free.isEmpty()) {
                UiFactory.warning(this, "No room is free for that date, time and group size "
                        + "(or you have a regular class then). Try another slot.");
            }
        } catch (AppException | IllegalArgumentException e) {
            UiFactory.error(this, e.getMessage());
        }
    }

    @Override
    protected void save() {
        RoomBooking booking = new RoomBooking();
        booking.setPurpose(purpose.getText());
        booking.setBookingDate(date.getDate("Date"));
        booking.setStartTime(parseTime(start.getSelectedItem(), "Start time"));
        booking.setEndTime(parseTime(end.getSelectedItem(), "End time"));
        booking.setExpectedAttendees(Validator.parseInt(attendees.getText(), "Expected number of people"));
        Classroom chosen = Validator.requireNotNull((Classroom) room.getSelectedItem(),
                "a room (press \"Find free rooms\" first)");
        booking.setClassroomId(chosen.getClassroomId());
        service.request(faculty, booking);
    }

    private static String[] timeChoices() {
        String[] times = new String[23];
        for (int i = 0; i < times.length; i++) {
            times[i] = LocalTime.of(7 + i / 2, (i % 2) * 30).toString();
        }
        return times;
    }

    private static LocalTime parseTime(Object value, String fieldName) {
        try {
            return LocalTime.parse(String.valueOf(value).trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(fieldName + " must look like 14:30 (HH:mm).");
        }
    }
}
