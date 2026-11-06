package com.smartclassroom.ui.common;

import com.smartclassroom.model.GradeCard;
import com.smartclassroom.model.Student;
import com.smartclassroom.model.User;
import com.smartclassroom.service.GradeService;
import com.smartclassroom.service.StudentService;
import com.smartclassroom.ui.theme.RoundedPanel;
import com.smartclassroom.ui.theme.Theme;
import com.smartclassroom.ui.theme.UiFactory;
import com.smartclassroom.util.CsvExporter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Admin and faculty: pick a class and see every student ranked by SGPA. */
public class RankingsPanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;

    /** One selectable class (branch + semester + section). */
    private record ClassChoice(String branch, int semester, String section) {
        @Override
        public String toString() {
            return branch + " - Semester " + semester + " - Section " + section;
        }
    }

    private final transient User actor;
    private final transient GradeService gradeService = new GradeService();
    private final transient StudentService studentService = new StudentService();
    private final JComboBox<ClassChoice> classBox = new JComboBox<>();
    private final AppTableModel model = new AppTableModel(
            new String[]{"Rank", "Roll no", "Student", "SGPA", "Percentage", "Result"});
    private final JTable table = UiFactory.table(model);
    private final JButton export = UiFactory.secondaryButton("Export CSV");
    private transient List<GradeCard> shown = new ArrayList<>();
    private boolean loading;

    public RankingsPanel(User actor) {
        super(new BorderLayout(0, 16));
        this.actor = actor;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel titles = new JPanel(new BorderLayout());
        titles.setOpaque(false);
        titles.add(UiFactory.title("Class Rankings"), BorderLayout.NORTH);
        titles.add(UiFactory.muted("Students ranked by SGPA, then by overall percentage"), BorderLayout.SOUTH);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controls.setOpaque(false);
        controls.add(classBox);
        controls.add(export);
        export.addActionListener(e -> exportCsv());
        classBox.addActionListener(e -> {
            if (!loading) {
                loadRanking();
            }
        });

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.add(titles, BorderLayout.WEST);
        heading.add(controls, BorderLayout.EAST);

        RoundedPanel card = new RoundedPanel(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        card.add(UiFactory.scroll(table), BorderLayout.CENTER);

        add(heading, BorderLayout.NORTH);
        add(card, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        try {
            loading = true;
            Object previous = classBox.getSelectedItem();
            Set<ClassChoice> classes = new LinkedHashSet<>();
            for (Student s : studentService.getAll(actor)) {
                classes.add(new ClassChoice(s.getBranch(), s.getSemester(), s.getSection()));
            }
            classBox.removeAllItems();
            classes.forEach(classBox::addItem);
            if (previous != null && classes.contains(previous)) {
                classBox.setSelectedItem(previous);
            }
            loading = false;
            loadRanking();
        } catch (RuntimeException e) {
            loading = false;
            UiFactory.showException(this, e);
        }
    }

    private void loadRanking() {
        try {
            model.setRowCount(0);
            shown = new ArrayList<>();
            ClassChoice choice = (ClassChoice) classBox.getSelectedItem();
            if (choice == null) {
                return;
            }
            shown = gradeService.getRankList(actor, choice.branch(), choice.semester(), choice.section());
            for (GradeCard c : shown) {
                Student s = c.getStudent();
                if (c.isEmpty()) {
                    model.addRow(new Object[]{"-", s.getRollNo(), s.getFullName(), "-", "-", "No marks yet"});
                } else {
                    model.addRow(new Object[]{c.getRank(), s.getRollNo(), s.getFullName(),
                            Math.round(c.getSgpa() * 100) / 100.0,
                            Math.round(c.getOverallPercentage() * 10) / 10.0, c.isPass() ? "Pass" : "Fail"});
                }
            }
        } catch (RuntimeException e) {
            UiFactory.showException(this, e);
        }
    }

    private void exportCsv() {
        ClassChoice choice = (ClassChoice) classBox.getSelectedItem();
        if (choice == null || shown.isEmpty()) {
            UiFactory.warning(this, "There is nothing to export.");
            return;
        }
        Path file = CsvFileChooser.ask(this, "rankings_" + choice.branch() + "_sem" + choice.semester()
                + "_" + choice.section() + ".csv");
        if (file == null) {
            return;
        }
        List<List<String>> rows = new ArrayList<>();
        for (GradeCard c : shown) {
            Student s = c.getStudent();
            rows.add(c.isEmpty()
                    ? List.of("", s.getRollNo(), s.getFullName(), "", "", "No marks yet")
                    : List.of(String.valueOf(c.getRank()), s.getRollNo(), s.getFullName(),
                    String.format("%.2f", c.getSgpa()), String.format("%.2f", c.getOverallPercentage()),
                    c.isPass() ? "Pass" : "Fail"));
        }
        CsvExporter.export(file, List.of("Rank", "Roll no", "Student", "SGPA", "Percentage", "Result"), rows);
        UiFactory.success(this, "Rankings saved to " + file.getFileName());
    }
}
