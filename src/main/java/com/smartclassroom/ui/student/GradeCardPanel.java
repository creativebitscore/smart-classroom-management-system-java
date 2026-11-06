package com.smartclassroom.ui.student;

import com.smartclassroom.model.GradeCard;
import com.smartclassroom.model.Student;
import com.smartclassroom.service.GradeService;
import com.smartclassroom.ui.common.AppTableModel;
import com.smartclassroom.ui.common.BarChartPanel;
import com.smartclassroom.ui.common.CsvFileChooser;
import com.smartclassroom.ui.common.Refreshable;
import com.smartclassroom.ui.common.StatCard;
import com.smartclassroom.ui.theme.RoundedPanel;
import com.smartclassroom.ui.theme.Theme;
import com.smartclassroom.ui.theme.UiFactory;
import com.smartclassroom.util.CsvExporter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** The student's result: SGPA, overall percentage, class rank, a grade for every subject and a bar chart. */
public class GradeCardPanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;
    private static final double PASS_MARK = 40;

    private final transient Student student;
    private final transient GradeService service = new GradeService();
    private final AppTableModel model = new AppTableModel(
            new String[]{"Code", "Subject", "Credits", "Marks", "Percentage", "Grade", "Points"});
    private final JTable table = UiFactory.table(model);
    private final StatCard sgpa = new StatCard("SGPA", Theme.PRIMARY);
    private final StatCard percentage = new StatCard("OVERALL PERCENTAGE", Theme.SUCCESS);
    private final StatCard rank = new StatCard("CLASS RANK", Theme.WARNING);
    private final StatCard result = new StatCard("RESULT", new Color(0x7C3AED));
    private final BarChartPanel chart = new BarChartPanel(PASS_MARK);
    private transient GradeCard current;

    public GradeCardPanel(Student student) {
        super(new BorderLayout(0, 18));
        this.student = student;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel titles = new JPanel(new BorderLayout());
        titles.setOpaque(false);
        titles.add(UiFactory.title("Grade Card"), BorderLayout.NORTH);
        titles.add(UiFactory.muted("Semester " + student.getSemester() + " - calculated from all your marks"),
                BorderLayout.SOUTH);
        JButton export = UiFactory.secondaryButton("Export CSV");
        export.addActionListener(e -> exportCsv());
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.add(titles, BorderLayout.WEST);
        heading.add(export, BorderLayout.EAST);

        JPanel cards = new JPanel(new GridLayout(1, 4, 16, 0));
        cards.setOpaque(false);
        cards.add(sgpa);
        cards.add(percentage);
        cards.add(rank);
        cards.add(result);

        RoundedPanel tableCard = new RoundedPanel(new BorderLayout(0, 8));
        tableCard.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        tableCard.add(UiFactory.heading("Subject-wise grades"), BorderLayout.NORTH);
        tableCard.add(UiFactory.scroll(table), BorderLayout.CENTER);

        RoundedPanel chartCard = new RoundedPanel(new BorderLayout(0, 8));
        chartCard.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        chartCard.setPreferredSize(new Dimension(420, 10));
        chartCard.add(UiFactory.heading("Percentage by subject"), BorderLayout.NORTH);
        chartCard.add(chart, BorderLayout.CENTER);
        chartCard.add(UiFactory.muted("Red bars are below the pass mark (" + (int) PASS_MARK + "%)."),
                BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setOpaque(false);
        body.add(tableCard, BorderLayout.CENTER);
        body.add(chartCard, BorderLayout.EAST);

        JPanel center = new JPanel(new BorderLayout(0, 18));
        center.setOpaque(false);
        center.add(cards, BorderLayout.NORTH);
        center.add(body, BorderLayout.CENTER);

        add(heading, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        try {
            current = service.getGradeCard(student);
            model.setRowCount(0);
            List<String> labels = new ArrayList<>();
            List<Double> values = new ArrayList<>();
            for (GradeCard.SubjectGrade s : current.getSubjects()) {
                model.addRow(new Object[]{s.subjectCode(), s.subjectName(), s.credits(),
                        format(s.obtained()) + " / " + format(s.max()), round(s.percentage()),
                        s.grade().getLetter(), s.grade().getPoints()});
                labels.add(s.subjectCode());
                values.add(s.percentage());
            }
            chart.setData(labels, values);
            if (current.isEmpty()) {
                sgpa.set("-", "no marks entered yet");
                percentage.set("-", " ");
                rank.set("-", " ");
                result.set("-", " ");
            } else {
                sgpa.set(String.format("%.2f", current.getSgpa()), "out of 10, credit weighted");
                percentage.set(String.format("%.1f%%", current.getOverallPercentage()), "all graded subjects");
                rank.set(current.getRank() + " / " + current.getClassSize(), "in section " + student.getSection());
                result.set(current.isPass() ? "PASS" : "FAIL",
                        current.isPass() ? "all subjects cleared" : "at least one subject below 40%");
            }
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        }
    }

    private void exportCsv() {
        if (current == null || current.isEmpty()) {
            UiFactory.warning(this, "There are no marks to export yet.");
            return;
        }
        Path file = CsvFileChooser.ask(this, "grade_card_" + student.getRollNo() + ".csv");
        if (file == null) {
            return;
        }
        List<List<String>> rows = new ArrayList<>();
        for (GradeCard.SubjectGrade s : current.getSubjects()) {
            rows.add(List.of(s.subjectCode(), s.subjectName(), String.valueOf(s.credits()),
                    format(s.obtained()), format(s.max()), String.format("%.2f", s.percentage()),
                    s.grade().getLetter(), String.valueOf(s.grade().getPoints())));
        }
        rows.add(List.of("SGPA", String.format("%.2f", current.getSgpa()), "", "", "",
                String.format("%.2f", current.getOverallPercentage()), "", ""));
        CsvExporter.export(file, List.of("Code", "Subject", "Credits", "Obtained", "Maximum", "Percentage",
                "Grade", "Points"), rows);
        UiFactory.success(this, "Grade card saved to " + file.getFileName());
    }

    private static String format(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format("%.1f", value);
    }

    private static double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
