package com.smartclassroom.ui.admin;

import com.smartclassroom.model.Classroom;
import com.smartclassroom.model.User;
import com.smartclassroom.service.ClassroomService;
import com.smartclassroom.ui.common.FormDialog;
import com.smartclassroom.util.Validator;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.awt.Window;

/** Add or edit a classroom. */
public class ClassroomDialog extends FormDialog {
    private static final long serialVersionUID = 1L;
    private final transient ClassroomService service = new ClassroomService();
    private final transient User actor;
    private final transient Classroom existing;

    private final JTextField name = form.addField("Room name (for example R101)", new JTextField(24));
    private final JTextField capacity = form.addField("Seating capacity", new JTextField(24));
    private final JComboBox<Classroom.RoomType> type =
            form.addField("Room type", new JComboBox<>(Classroom.RoomType.values()));

    public ClassroomDialog(Window owner, User actor, Classroom existing) {
        super(owner, existing == null ? "Add classroom" : "Edit classroom");
        this.actor = actor;
        this.existing = existing;
        if (existing != null) {
            name.setText(existing.getRoomName());
            capacity.setText(String.valueOf(existing.getCapacity()));
            type.setSelectedItem(existing.getRoomType());
        }
    }

    @Override
    protected void save() {
        int seats = Validator.parseInt(capacity.getText(), "Capacity");
        Validator.requireIntInRange(seats, "Capacity", 1, 1000);
        Classroom room = existing == null ? new Classroom() : existing;
        room.setRoomName(name.getText());
        room.setCapacity(seats);
        room.setRoomType((Classroom.RoomType) type.getSelectedItem());
        if (existing == null) {
            service.add(actor, room);
        } else {
            service.update(actor, room);
        }
    }
}
