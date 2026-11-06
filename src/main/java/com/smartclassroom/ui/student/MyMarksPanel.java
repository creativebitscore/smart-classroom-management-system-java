package com.smartclassroom.ui.student;

import com.smartclassroom.model.Mark;
import com.smartclassroom.model.Student;
import com.smartclassroom.service.MarkService;
import com.smartclassroom.ui.common.AppTableModel;
import com.smartclassroom.ui.common.Percent;
import com.smartclassroom.ui.common.Refreshable;
import com.smartclassroom.ui.theme.RoundedPanel;
import com.smartclassroom.ui.theme.Theme;
import com.smartclassroom.ui.theme.UiFactory;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/** The student's marks in every exam. */
public class MyMarksPanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;
    private final transient Student student;
    private final transient MarkService service = new MarkService();
    private final AppTableModel model =
            new AppTableModel(new String[]{"Subject", "Exam", "Marks", "Out of", "Percentage"});

    public MyMarksPanel(Student student) {
        super(new BorderLayout(0, 16));
        this.student = student;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        JPanel titles = new JPanel(new BorderLayout());
        titles.setOpaque(false);
        titles.add(UiFactory.title("My Marks"), BorderLayout.NORTH);
        titles.add(UiFactory.muted("Academic record entered by your faculty"), BorderLayout.SOUTH);

        RoundedPanel card = new RoundedPanel(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        card.add(UiFactory.scroll(UiFactory.table(model)), BorderLayout.CENTER);
        add(titles, BorderLayout.NORTH);
        add(card, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        try {
            model.setRowCount(0);
            for (Mark m : service.getForStudent(student)) {
                model.addRow(new Object[]{m.getSubjectName(), m.getExamType(), m.getMarksObtained(), m.getMaxMarks(),
                        new Percent(m.getPercentage())});
            }
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        }
    }
}
