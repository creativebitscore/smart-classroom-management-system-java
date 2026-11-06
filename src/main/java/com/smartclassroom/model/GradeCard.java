package com.smartclassroom.model;

import java.util.ArrayList;
import java.util.List;

/** A student's result for the current semester: one line per subject plus SGPA, percentage and class rank. */
public class GradeCard {

    /** One subject line of the grade card. */
    public record SubjectGrade(int subjectId, String subjectCode, String subjectName, int credits,
                               double obtained, double max) {
        public double percentage() {
            return max <= 0 ? 0 : obtained * 100.0 / max;
        }

        public GradeScale grade() {
            return GradeScale.forPercentage(percentage());
        }
    }

    private final Student student;
    private final List<SubjectGrade> subjects = new ArrayList<>();
    private int rank;
    private int classSize;

    public GradeCard(Student student) {
        this.student = student;
    }

    public void add(SubjectGrade grade) {
        subjects.add(grade);
    }

    public Student getStudent() {
        return student;
    }

    public List<SubjectGrade> getSubjects() {
        return subjects;
    }

    public boolean isEmpty() {
        return subjects.isEmpty();
    }

    /** Semester Grade Point Average = sum(credits x grade points) / sum(credits). 0 when nothing is graded. */
    public double getSgpa() {
        int credits = 0;
        double weighted = 0;
        for (SubjectGrade s : subjects) {
            credits += s.credits();
            weighted += s.credits() * s.grade().getPoints();
        }
        return credits == 0 ? 0 : weighted / credits;
    }

    /** Total marks obtained as a percentage of the total maximum marks of all graded subjects. */
    public double getOverallPercentage() {
        double obtained = 0;
        double max = 0;
        for (SubjectGrade s : subjects) {
            obtained += s.obtained();
            max += s.max();
        }
        return max <= 0 ? 0 : obtained * 100.0 / max;
    }

    /** Pass only if every graded subject is passed. */
    public boolean isPass() {
        return !subjects.isEmpty() && subjects.stream().allMatch(s -> s.grade().isPass());
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getClassSize() {
        return classSize;
    }

    public void setClassSize(int classSize) {
        this.classSize = classSize;
    }
}
