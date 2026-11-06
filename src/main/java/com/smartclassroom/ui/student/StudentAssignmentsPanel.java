package com.smartclassroom.ui.student;

import com.smartclassroom.model.Assignment;
import com.smartclassroom.model.Student;
import com.smartclassroom.service.AssignmentService;
import com.smartclassroom.ui.common.AppTableModel;
import com.smartclassroom.ui.common.Refreshable;
import com.smartclassroom.ui.theme.RoundedPanel;
import com.smartclassroom.ui.theme.Theme;
import com.smartclassroom.ui.theme.UiFactory;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** Assignments published for the student's class, with due dates and status. */
public class StudentAssignmentsPanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;
    private final transient Student student;
    private final transient AssignmentService service = new AssignmentService();
    private final AppTableModel model =
            new AppTableModel(new String[]{"Subject", "Title", "Faculty", "Due date", "Status"});
    private final JTable table = UiFactory.table(model);
    private final JTextArea detail = new JTextArea();
    private transient List<Assignment> items = new ArrayList<>();

    public StudentAssignmentsPanel(Student student) {
        super(new BorderLayout(0, 16));
        this.student = student;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel titles = new JPanel(new BorderLayout());
        titles.setOpaque(false);
        titles.add(UiFactory.title("Assignments"), BorderLayout.NORTH);
        titles.add(UiFactory.muted("Sorted by due date. Select one to read the instructions."), BorderLayout.SOUTH);

        detail.setEditable(false);
        detail.setLineWrap(true);
        detail.setWrapStyleWord(true);
        detail.setFont(Theme.FONT_BASE);
        detail.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        detail.setText("Select an assignment to read its description.");
        var detailScroll = UiFactory.scroll(detail);
        detailScroll.setPreferredSize(new Dimension(10, 120));

        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        card.add(UiFactory.scroll(table), BorderLayout.CENTER);
        card.add(detailScroll, BorderLayout.SOUTH);

        add(titles, BorderLayout.NORTH);
        add(card, BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                Assignment a = items.get(table.convertRowIndexToModel(row));
                String text = a.getDescription() == null || a.getDescription().isBlank()
                        ? "No description was given." : a.getDescription();
                detail.setText(a.getTitle() + "\n\n" + text);
                detail.setCaretPosition(0);
            }
        });
    }

    @Override
    public void refresh() {
        try {
            items = service.getForStudent(student);
            LocalDate today = LocalDate.now();
            model.setRowCount(0);
            for (Assignment a : items) {
                String status;
                if (a.isOverdue(today)) {
                    status = "Overdue";
                } else {
                    long days = ChronoUnit.DAYS.between(today, a.getDueDate());
                    status = days == 0 ? "Due today" : "Due in " + days + " days";
                }
                model.addRow(new Object[]{a.getSubjectName(), a.getTitle(), a.getFacultyName(), a.getDueDate(), status});
            }
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        }
    }
}
