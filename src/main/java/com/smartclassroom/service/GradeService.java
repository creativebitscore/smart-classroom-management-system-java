package com.smartclassroom.service;

import com.smartclassroom.dao.MarkDao;
import com.smartclassroom.dao.StudentDao;
import com.smartclassroom.dao.SubjectDao;
import com.smartclassroom.model.GradeCard;
import com.smartclassroom.model.Mark;
import com.smartclassroom.model.Role;
import com.smartclassroom.model.Student;
import com.smartclassroom.model.Subject;
import com.smartclassroom.model.User;
import com.smartclassroom.util.Validator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the raw marks into a grade card: per-subject percentage, letter grade, SGPA and class rank.
 * Nothing is stored; everything is calculated from the marks table, so it can never be out of date.
 *  - a subject's percentage = all its exams added together (MID1 + MID2 + ...) / their maximum
 *  - SGPA is weighted by the subject credits
 *  - rank: higher SGPA first, then higher overall percentage; equal students share a rank (1, 2, 2, 4)
 *  - only subjects of the student's current branch and semester that have at least one mark are counted
 */
public class GradeService {
    private static final double EPSILON = 1e-9;

    private final MarkDao markDao;
    private final SubjectDao subjectDao;
    private final StudentDao studentDao;

    public GradeService() {
        this(new MarkDao(), new SubjectDao(), new StudentDao());
    }

    public GradeService(MarkDao markDao, SubjectDao subjectDao, StudentDao studentDao) {
        this.markDao = markDao;
        this.subjectDao = subjectDao;
        this.studentDao = studentDao;
    }

    /** The logged-in student's own grade card, including the rank among classmates of the same section. */
    public GradeCard getGradeCard(User actor) {
        AccessControl.requireRole(actor, Role.STUDENT);
        Student student = (Student) actor;
        Map<Integer, Subject> subjects = subjectsOf(student.getBranch(), student.getSemester());
        List<GradeCard> ranked = rankClass(studentDao.findByClass(student.getBranch(), student.getSemester(),
                student.getSection()), subjects);
        for (GradeCard card : ranked) {
            if (card.getStudent().getStudentId() == student.getStudentId()) {
                return card;
            }
        }
        GradeCard own = build(student, subjects);
        own.setClassSize((int) ranked.stream().filter(c -> c.getRank() > 0).count());
        return own;
    }

    /** Ranked grade cards of one class for admin and faculty (students without marks come last, rank 0). */
    public List<GradeCard> getRankList(User actor, String branch, int semester, String section) {
        AccessControl.requireRole(actor, Role.ADMIN, Role.FACULTY);
        String cleanBranch = Validator.requireText(branch, "Branch", 50);
        String cleanSection = Validator.requireText(section, "Section", 5);
        return rankClass(studentDao.findByClass(cleanBranch, semester, cleanSection),
                subjectsOf(cleanBranch, semester));
    }

    // ---------- calculation ----------

    private Map<Integer, Subject> subjectsOf(String branch, int semester) {
        Map<Integer, Subject> byId = new LinkedHashMap<>();
        for (Subject s : subjectDao.findByBranchAndSemester(branch, semester)) {
            byId.put(s.getSubjectId(), s);
        }
        return byId;
    }

    GradeCard build(Student student, Map<Integer, Subject> subjects) {
        Map<Integer, double[]> totals = new HashMap<>(); // subjectId -> {obtained, max}
        for (Mark m : markDao.findByStudent(student.getStudentId())) {
            if (subjects.containsKey(m.getSubjectId())) {
                double[] sum = totals.computeIfAbsent(m.getSubjectId(), id -> new double[2]);
                sum[0] += m.getMarksObtained();
                sum[1] += m.getMaxMarks();
            }
        }
        GradeCard card = new GradeCard(student);
        for (Subject s : subjects.values()) {
            double[] sum = totals.get(s.getSubjectId());
            if (sum != null) {
                card.add(new GradeCard.SubjectGrade(s.getSubjectId(), s.getSubjectCode(), s.getSubjectName(),
                        s.getCredits(), sum[0], sum[1]));
            }
        }
        return card;
    }

    List<GradeCard> rankClass(List<Student> students, Map<Integer, Subject> subjects) {
        List<GradeCard> withResults = new ArrayList<>();
        List<GradeCard> withoutResults = new ArrayList<>();
        for (Student s : students) {
            GradeCard card = build(s, subjects);
            (card.isEmpty() ? withoutResults : withResults).add(card);
        }
        withResults.sort(Comparator.comparingDouble(GradeCard::getSgpa).reversed()
                .thenComparing(Comparator.comparingDouble(GradeCard::getOverallPercentage).reversed()));
        for (int i = 0; i < withResults.size(); i++) {
            GradeCard card = withResults.get(i);
            boolean tie = i > 0 && sameResult(withResults.get(i - 1), card);
            card.setRank(tie ? withResults.get(i - 1).getRank() : i + 1);
        }
        List<GradeCard> all = new ArrayList<>(withResults);
        all.addAll(withoutResults);
        for (GradeCard card : all) {
            card.setClassSize(withResults.size());
        }
        return all;
    }

    private static boolean sameResult(GradeCard a, GradeCard b) {
        return Math.abs(a.getSgpa() - b.getSgpa()) < EPSILON
                && Math.abs(a.getOverallPercentage() - b.getOverallPercentage()) < EPSILON;
    }
}
