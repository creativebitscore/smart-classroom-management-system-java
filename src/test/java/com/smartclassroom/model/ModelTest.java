package com.smartclassroom.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelTest {

    // ----- attendance calculation -----

    @Test
    void percentageIsAttendedOverTotal() {
        assertEquals(75.0, AttendanceSummary.calculatePercentage(3, 4), 0.0001);
        assertEquals(100.0, AttendanceSummary.calculatePercentage(5, 5), 0.0001);
        assertEquals(0.0, AttendanceSummary.calculatePercentage(0, 5), 0.0001);
    }

    @Test
    void noClassesGivesZeroInsteadOfDivisionError() {
        assertEquals(0.0, AttendanceSummary.calculatePercentage(0, 0), 0.0001);
    }

    @Test
    void summaryUsesAttendedClassesIncludingLate() {
        // 10 classes: 6 present + 2 late = 8 attended
        AttendanceSummary s = new AttendanceSummary(1, "CSE1", "Aarav", "CS301", "Data Structures", 10, 8, 2);
        assertEquals(80.0, s.getPercentage(), 0.0001);
    }

    @Test
    void thresholdBoundaryIsInclusive() {
        AttendanceSummary exactly75 = new AttendanceSummary(1, "R", "N", "C", "S", 4, 3, 0);
        AttendanceSummary below = new AttendanceSummary(2, "R", "N", "C", "S", 4, 2, 0);
        assertFalse(exactly75.isBelowThreshold(75));
        assertTrue(below.isBelowThreshold(75));
    }

    @Test
    void studentWithNoClassesIsNeverFlagged() {
        AttendanceSummary none = new AttendanceSummary(1, "R", "N", "C", "S", 0, 0, 0);
        assertFalse(none.isBelowThreshold(75));
    }

    @Test
    void overallPercentageCombinesAllSubjects() {
        List<AttendanceSummary> list = List.of(
                new AttendanceSummary(1, "R", "N", "A", "A", 4, 4, 0),
                new AttendanceSummary(1, "R", "N", "B", "B", 4, 0, 0));
        assertEquals(50.0, AttendanceSummary.overallPercentage(list), 0.0001);
    }

    @Test
    void onlyPresentAndLateCountAsAttended() {
        assertTrue(AttendanceStatus.PRESENT.countsAsAttended());
        assertTrue(AttendanceStatus.LATE.countsAsAttended());
        assertFalse(AttendanceStatus.ABSENT.countsAsAttended());
    }

    // ----- polymorphism and encapsulation -----

    @Test
    void eachRoleSuppliesItsOwnMenuAndTitle() {
        User admin = new Admin();
        User faculty = new Faculty();
        User student = new Student();
        assertNotEquals(admin.getMenuItems(), faculty.getMenuItems());
        assertNotEquals(faculty.getMenuItems(), student.getMenuItems());
        assertTrue(admin.getMenuItems().contains(Admin.MENU_REPORTS));
        assertFalse(student.getMenuItems().contains(Admin.MENU_REPORTS));
        assertTrue(faculty.getMenuItems().contains(Faculty.MENU_MARK_ATTENDANCE));
        assertFalse(student.getMenuItems().contains(Faculty.MENU_MARK_ATTENDANCE));
        assertEquals("Admin Console", admin.getDashboardTitle());
        assertEquals("Faculty Portal", faculty.getDashboardTitle());
        assertEquals("Student Portal", student.getDashboardTitle());
    }

    @Test
    void subclassesCarryTheirRole() {
        assertEquals(Role.ADMIN, new Admin().getRole());
        assertEquals(Role.FACULTY, new Faculty().getRole());
        assertEquals(Role.STUDENT, new Student().getRole());
    }

    @Test
    void classroomRefusesNonPositiveCapacity() {
        Classroom room = new Classroom();
        assertThrows(IllegalArgumentException.class, () -> room.setCapacity(0));
        assertThrows(IllegalArgumentException.class, () -> room.setCapacity(-10));
        room.setCapacity(30);
        assertEquals(30, room.getCapacity());
    }

    @Test
    void assignmentKnowsWhenItIsOverdue() {
        Assignment a = new Assignment();
        a.setDueDate(LocalDate.of(2026, 11, 1));
        assertTrue(a.isOverdue(LocalDate.of(2026, 11, 2)));
        assertFalse(a.isOverdue(LocalDate.of(2026, 11, 1)));
    }
}
