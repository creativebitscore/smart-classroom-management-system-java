package com.smartclassroom.ui.admin;

import com.smartclassroom.model.Classroom;
import com.smartclassroom.model.User;
import com.smartclassroom.service.ClassroomService;
import com.smartclassroom.ui.common.CrudPanel;
import com.smartclassroom.ui.theme.UiFactory;

import java.util.List;

/** Admin screen for classrooms and capacities. */
public class ClassroomPanel extends CrudPanel<Classroom> {
    private static final long serialVersionUID = 1L;
    private final transient User actor;
    private final transient ClassroomService service = new ClassroomService();

    public ClassroomPanel(User actor) {
        super("Classrooms", "Rooms and their seating capacity",
                new String[]{"Room", "Capacity", "Type"}, true, true, true);
        this.actor = actor;
    }

    @Override
    protected List<Classroom> loadItems() {
        return service.getAll();
    }

    @Override
    protected Object[] toRow(Classroom c) {
        return new Object[]{c.getRoomName(), c.getCapacity(), c.getRoomType()};
    }

    @Override
    protected void onAdd() {
        if (new ClassroomDialog(owner(), actor, null).showDialog()) {
            UiFactory.success(this, "Classroom added successfully.");
            refresh();
        }
    }

    @Override
    protected void onEdit(Classroom c) {
        if (new ClassroomDialog(owner(), actor, c).showDialog()) {
            UiFactory.success(this, "Classroom updated successfully.");
            refresh();
        }
    }

    @Override
    protected void onDelete(Classroom c) {
        service.delete(actor, c.getClassroomId());
    }

    @Override
    protected String deleteMessage(Classroom c) {
        return "Delete room " + c.getRoomName() + "?\n\nTimetable entries that use this room will also be removed.";
    }
}
