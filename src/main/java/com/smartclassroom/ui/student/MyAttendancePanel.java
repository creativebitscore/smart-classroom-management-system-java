package com.smartclassroom.ui.student;

import com.smartclassroom.model.AttendanceSummary;
import com.smartclassroom.model.Student;
import com.smartclassroom.service.AttendanceService;
import com.smartclassroom.ui.common.AppTableModel;
import com.smartclassroom.ui.common.Percent;
import com.smartclassroom.ui.common.Refreshable;
import com.smartclassroom.ui.common.StatCard;
import com.smartclassroom.ui.theme.RoundedPanel;
import com.smartclassroom.ui.theme.Theme;
import com.smartclassroom.ui.theme.UiFactory;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/** The student's attendance percentage, overall and per subject. */
public class MyAttendancePanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;
    private final transient Student student;
    private final transient AttendanceService service = new AttendanceService();
    private final StatCard overallCard = new StatCard("OVERALL ATTENDANCE", Theme.PRIMARY);
    private final AppTableModel model = new AppTableModel(
            new String[]{"Subject", "Classes held", "Attended", "Late", "Attendance", "Status"});
    private final JTable table = UiFactory.table(model);
    private transient List<AttendanceSummary> summaries = new ArrayList<>();
    private int threshold = AttendanceService.DEFAULT_THRESHOLD;

    public MyAttendancePanel(Student student) {
        super(new BorderLayout(0, 16));
        this.student = student;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel titles = new JPanel(new BorderLayout());
        titles.setOpaque(false);
        titles.add(UiFactory.title("My Attendance"), BorderLayout.NORTH);
        titles.add(UiFactory.muted("Late counts as attended. Subjects under the minimum are shown in red."),
                BorderLayout.SOUTH);

        JPanel cardRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        cardRow.setOpaque(false);
        cardRow.add(overallCard);

        JPanel north = new JPanel(new BorderLayout(0, 16));
        north.setOpaque(false);
        north.add(titles, BorderLayout.NORTH);
        north.add(cardRow, BorderLayout.SOUTH);

        RoundedPanel card = new RoundedPanel(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        card.add(UiFactory.scroll(table), BorderLayout.CENTER);

        add(north, BorderLayout.NORTH);
        add(card, BorderLayout.CENTER);
        UiFactory.highlightRows(table, row -> row < summaries.size() && summaries.get(row).isBelowThreshold(threshold),
                Theme.DANGER_BG);
    }

    @Override
    public void refresh() {
        try {
            threshold = service.getThreshold();
            summaries = service.getSummaryForStudent(student);
            model.setRowCount(0);
            for (AttendanceSummary s : summaries) {
                model.addRow(new Object[]{s.getSubjectCode() + " - " + s.getSubjectName(), s.getTotalClasses(),
                        s.getAttendedClasses(), s.getLateClasses(), new Percent(s.getPercentage()),
                        s.isBelowThreshold(threshold) ? "Below " + threshold + "%" : "OK"});
            }
            if (summaries.isEmpty()) {
                overallCard.set("-", "no classes recorded yet");
            } else {
                double overall = AttendanceSummary.overallPercentage(summaries);
                overallCard.set(String.format("%.1f%%", overall),
                        overall < threshold ? "below the required " + threshold + "%" : "meets the required " + threshold + "%");
            }
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        }
    }
}
