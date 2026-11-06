package com.smartclassroom.service;

import com.smartclassroom.model.Faculty;
import com.smartclassroom.model.GradeCard;
import com.smartclassroom.model.GradeScale;
import com.smartclassroom.model.Mark;
import com.smartclassroom.model.Student;
import com.smartclassroom.model.Subject;
import com.smartclassroom.service.exception.AccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Grade scale, SGPA, overall percentage, pass/fail and ranking (including ties). */
class GradeServiceTest {
    private final com.smartclassroom.model.Admin admin = Fakes.admin();
    private Faculty ravi;
    private Fakes.FakeMarkDao markDao;
    private Fakes.FakeSubjectDao subjectDao;
    private Fakes.FakeStudentDao studentDao;
    private GradeService service;
    private Student aarav;
    private Student priya;
    private Student rahul;
    private Student sneha;

    @BeforeEach
    void setUp() {
        ravi = Fakes.faculty(1, 2, "Dr. Ravi Kumar");
        markDao = new Fakes.FakeMarkDao();
        subjectDao = new Fakes.FakeSubjectDao();
        studentDao = new Fakes.FakeStudentDao();
        service = new GradeService(markDao, subjectDao, studentDao);

        Subject ds = Fakes.subject(1, "CS301", "Data Structures"); // 4 credits
        Subject maths = Fakes.subject(4, "MA301", "Discrete Mathematics");
        maths.setCredits(3);
        subjectDao.subjects.put(1, ds);
        subjectDao.subjects.put(4, maths);

        aarav = Fakes.student(1, 5, "Aarav");
        priya = Fakes.student(2, 6, "Priya");
        rahul = Fakes.student(3, 7, "Rahul");
        sneha = Fakes.student(4, 8, "Sneha");
        studentDao.students.addAll(List.of(aarav, priya, rahul, sneha));
    }

    private void marks(Student s, int subjectId, String exam, double obtained, double max) {
        Mark m = new Mark();
        m.setStudentId(s.getStudentId());
        m.setSubjectId(subjectId);
        m.setExamType(exam);
        m.setMarksObtained(obtained);
        m.setMaxMarks(max);
        markDao.byStudent.computeIfAbsent(s.getStudentId(), id -> new ArrayList<>()).add(m);
    }

    // ----- GradeScale -----

    @Test
    void gradeBoundaries() {
        assertEquals(GradeScale.O, GradeScale.forPercentage(90));
        assertEquals(GradeScale.A_PLUS, GradeScale.forPercentage(89.99));
        assertEquals(GradeScale.A, GradeScale.forPercentage(70));
        assertEquals(GradeScale.B_PLUS, GradeScale.forPercentage(60));
        assertEquals(GradeScale.B, GradeScale.forPercentage(50));
        assertEquals(GradeScale.C, GradeScale.forPercentage(45));
        assertEquals(GradeScale.P, GradeScale.forPercentage(40));
        assertEquals(GradeScale.F, GradeScale.forPercentage(39.99));
        assertEquals(GradeScale.F, GradeScale.forPercentage(0));
        assertEquals(GradeScale.F, GradeScale.forPercentage(-5));
        assertEquals(GradeScale.O, GradeScale.forPercentage(100));
        assertEquals(10, GradeScale.O.getPoints());
        assertTrue(GradeScale.P.isPass());
        assertFalse(GradeScale.F.isPass());
    }

    // ----- grade card maths -----

    @Test
    void sgpaIsWeightedByCredits() {
        marks(aarav, 1, "MID1", 90, 100); // O  -> 10 points x 4 credits
        marks(aarav, 4, "MID1", 70, 100); // A  ->  8 points x 3 credits
        GradeCard card = service.getGradeCard(aarav);
        assertEquals((10 * 4 + 8 * 3) / 7.0, card.getSgpa(), 1e-9);
        assertEquals(80.0, card.getOverallPercentage(), 1e-9);
        assertTrue(card.isPass());
        assertEquals(2, card.getSubjects().size());
    }

    @Test
    void severalExamsOfOneSubjectAreAddedTogether() {
        marks(aarav, 1, "MID1", 20, 25);
        marks(aarav, 1, "MID2", 15, 25); // 35 / 50 = 70 %
        GradeCard card = service.getGradeCard(aarav);
        assertEquals(1, card.getSubjects().size());
        assertEquals(70.0, card.getSubjects().get(0).percentage(), 1e-9);
        assertEquals(GradeScale.A, card.getSubjects().get(0).grade());
    }

    @Test
    void oneFailedSubjectMakesTheResultFail() {
        marks(aarav, 1, "MID1", 80, 100);
        marks(aarav, 4, "MID1", 30, 100); // F
        GradeCard card = service.getGradeCard(aarav);
        assertFalse(card.isPass());
        assertEquals((9 * 4 + 0 * 3) / 7.0, card.getSgpa(), 1e-9);
    }

    @Test
    void marksOfOtherSemestersAreIgnored() {
        marks(aarav, 1, "MID1", 90, 100);
        marks(aarav, 77, "MID1", 5, 100); // subject 77 does not belong to this semester
        assertEquals(1, service.getGradeCard(aarav).getSubjects().size());
    }

    @Test
    void studentWithoutMarksHasAnEmptyCard() {
        GradeCard card = service.getGradeCard(aarav);
        assertTrue(card.isEmpty());
        assertEquals(0, card.getSgpa(), 1e-9);
        assertFalse(card.isPass());
        assertEquals(0, card.getRank());
    }

    // ----- ranking -----

    @Test
    void ranksAreBySgpaThenPercentage() {
        marks(aarav, 1, "MID1", 70, 100);
        marks(priya, 1, "MID1", 95, 100);
        marks(rahul, 1, "MID1", 82, 100);
        // sneha has no marks
        GradeCard aaravCard = service.getGradeCard(aarav);
        assertEquals(3, aaravCard.getRank());
        assertEquals(3, aaravCard.getClassSize()); // students without marks are not counted
        assertEquals(1, service.getGradeCard(priya).getRank());
        assertEquals(2, service.getGradeCard(rahul).getRank());
        assertEquals(0, service.getGradeCard(sneha).getRank());
    }

    @Test
    void studentsWithIdenticalResultsShareARank() {
        marks(aarav, 1, "MID1", 85, 100);
        marks(priya, 1, "MID1", 85, 100);
        marks(rahul, 1, "MID1", 60, 100);
        assertEquals(1, service.getGradeCard(aarav).getRank());
        assertEquals(1, service.getGradeCard(priya).getRank());
        assertEquals(3, service.getGradeCard(rahul).getRank()); // 1, 1, 3
    }

    @Test
    void sameSgpaIsSplitByPercentage() {
        marks(aarav, 1, "MID1", 82, 100); // A+ both
        marks(priya, 1, "MID1", 88, 100);
        assertEquals(2, service.getGradeCard(aarav).getRank());
        assertEquals(1, service.getGradeCard(priya).getRank());
    }

    @Test
    void rankListIsSortedWithUnmarkedStudentsLast() {
        marks(aarav, 1, "MID1", 70, 100);
        marks(priya, 1, "MID1", 95, 100);
        List<GradeCard> list = service.getRankList(ravi, "CSE", 3, "A");
        assertEquals(List.of("Priya", "Aarav"),
                list.subList(0, 2).stream().map(c -> c.getStudent().getFullName()).toList());
        assertTrue(list.get(2).isEmpty());
        assertTrue(list.get(3).isEmpty());
        assertEquals(4, list.size());
    }

    // ----- access -----

    @Test
    void rankListIsForStaffAndGradeCardForStudents() {
        assertThrows(AccessDeniedException.class, () -> service.getRankList(aarav, "CSE", 3, "A"));
        assertThrows(AccessDeniedException.class, () -> service.getGradeCard(ravi));
        assertThrows(AccessDeniedException.class, () -> service.getGradeCard(admin));
        assertEquals(4, service.getRankList(admin, "CSE", 3, "A").size());
    }
}
