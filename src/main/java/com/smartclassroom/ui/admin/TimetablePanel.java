package com.smartclassroom.ui.admin;

import com.smartclassroom.model.TimetableEntry;
import com.smartclassroom.model.User;
import com.smartclassroom.service.TimetableService;
import com.smartclassroom.ui.common.CrudPanel;
import com.smartclassroom.ui.theme.UiFactory;

import java.util.List;

/** The list view of the timetable with add / edit / delete. Clash and capacity checks happen in TimetableService. */
public class TimetablePanel extends CrudPanel<TimetableEntry> {
    private static final long serialVersionUID = 1L;
    private final transient User actor;
    private final transient TimetableService service = new TimetableService();
    private final transient Runnable onChanged;

    public TimetablePanel(User actor, Runnable onChanged) {
        super("Timetable", "Allocate classrooms and teachers; clashes are rejected automatically",
                new String[]{"Day", "Time", "Subject", "Faculty", "Room", "Section"}, true, true, true);
        this.actor = actor;
        this.onChanged = onChanged;
    }

    @Override
    protected List<TimetableEntry> loadItems() {
        return service.getAll();
    }

    @Override
    protected Object[] toRow(TimetableEntry t) {
        String day = t.getDayOfWeek().toString();
        return new Object[]{day.charAt(0) + day.substring(1).toLowerCase(), t.getSlotLabel(), t.getSubject().toString(),
                t.getFaculty().getFullName(), t.getClassroom().getRoomName(), t.getSection()};
    }

    @Override
    protected void onAdd() {
        if (new TimetableDialog(owner(), actor, null).showDialog()) {
            UiFactory.success(this, "Class scheduled successfully.");
            onChanged.run();
        }
    }

    @Override
    protected void onEdit(TimetableEntry t) {
        if (new TimetableDialog(owner(), actor, t).showDialog()) {
            UiFactory.success(this, "Timetable updated successfully.");
            onChanged.run();
        }
    }

    @Override
    protected void onDelete(TimetableEntry t) {
        service.delete(actor, t.getTimetableId());
        onChanged.run();
    }

    @Override
    protected String deleteMessage(TimetableEntry t) {
        return "Remove " + t.getSubject().getSubjectCode() + " on " + t.getDayOfWeek() + " " + t.getSlotLabel()
                + " from the timetable?";
    }
}
