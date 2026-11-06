package com.smartclassroom.service;

import com.smartclassroom.model.Classroom;
import com.smartclassroom.model.Faculty;
import com.smartclassroom.model.Subject;
import com.smartclassroom.model.TimetableEntry;
import com.smartclassroom.service.exception.AccessDeniedException;
import com.smartclassroom.service.exception.ConflictException;
import com.smartclassroom.service.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimetableServiceTest {
    private Fakes.FakeTimetableDao timetableDao;
    private Fakes.FakeStudentDao studentDao;
    private TimetableService service;
    private final com.smartclassroom.model.Admin admin = Fakes.admin();
    private Subject java;
    private Faculty ravi;
    private Classroom r101;

    @BeforeEach
    void setUp() {
        timetableDao = new Fakes.FakeTimetableDao();
        studentDao = new Fakes.FakeStudentDao();
        Fakes.FakeSubjectDao subjectDao = new Fakes.FakeSubjectDao();
        java = Fakes.subject(3, "CS303", "Object Oriented Java");
        subjectDao.subjects.put(3, java);
        ravi = Fakes.faculty(1, 2, "Dr. Ravi Kumar");
        r101 = Fakes.room(1, "R101", 60);
        studentDao.classSize = 6;
        service = new TimetableService(timetableDao, subjectDao, studentDao);
    }

    private TimetableEntry newEntry() {
        return Fakes.entry(java, ravi, r101, "a", DayOfWeek.FRIDAY, "09:00", "10:00");
    }

    @Test
    void validEntryIsSaved() {
        TimetableEntry e = newEntry();
        service.add(admin, e);
        assertEquals(1, timetableDao.inserted.size());
        assertEquals(901, e.getTimetableId());
        assertEquals("A", e.getSection());
    }

    @Test
    void roomAlreadyBookedIsRejected() {
        TimetableEntry other = Fakes.entry(Fakes.subject(1, "CS301", "Data Structures"), Fakes.faculty(2, 3, "Prof. Anita"),
                r101, "B", DayOfWeek.FRIDAY, "09:30", "10:30");
        timetableDao.roomConflicts = List.of(other);
        ConflictException e = assertThrows(ConflictException.class, () -> service.add(admin, newEntry()));
        assertTrue(e.getMessage().contains("R101"));
        assertTrue(timetableDao.inserted.isEmpty());
    }

    @Test
    void facultyTeachingTwoClassesAtOnceIsRejected() {
        TimetableEntry other = Fakes.entry(Fakes.subject(1, "CS301", "Data Structures"), ravi,
                Fakes.room(2, "R102", 40), "B", DayOfWeek.FRIDAY, "09:00", "10:00");
        timetableDao.facultyConflicts = List.of(other);
        ConflictException e = assertThrows(ConflictException.class, () -> service.add(admin, newEntry()));
        assertTrue(e.getMessage().contains("Dr. Ravi Kumar"));
        assertTrue(timetableDao.inserted.isEmpty());
    }

    @Test
    void sectionWithTwoLecturesAtOnceIsRejected() {
        TimetableEntry other = Fakes.entry(Fakes.subject(1, "CS301", "Data Structures"), Fakes.faculty(2, 3, "Prof. Anita"),
                Fakes.room(2, "R102", 40), "A", DayOfWeek.FRIDAY, "09:00", "10:00");
        timetableDao.classConflicts = List.of(other);
        ConflictException e = assertThrows(ConflictException.class, () -> service.add(admin, newEntry()));
        assertTrue(e.getMessage().contains("Section A"));
    }

    @Test
    void classLargerThanRoomIsRejected() {
        studentDao.classSize = 35;
        Classroom lab = Fakes.room(3, "LAB-1", 30);
        TimetableEntry e = Fakes.entry(java, ravi, lab, "A", DayOfWeek.FRIDAY, "09:00", "10:00");
        ValidationException ex = assertThrows(ValidationException.class, () -> service.add(admin, e));
        assertTrue(ex.getMessage().contains("30"));
        assertTrue(ex.getMessage().contains("35"));
        assertTrue(timetableDao.inserted.isEmpty());
    }

    @Test
    void classExactlyEqualToCapacityIsAllowed() {
        studentDao.classSize = 30;
        TimetableEntry e = Fakes.entry(java, ravi, Fakes.room(3, "LAB-1", 30), "A", DayOfWeek.FRIDAY, "09:00", "10:00");
        service.add(admin, e);
        assertEquals(1, timetableDao.inserted.size());
    }

    @Test
    void endBeforeOrEqualToStartIsRejected() {
        TimetableEntry backwards = Fakes.entry(java, ravi, r101, "A", DayOfWeek.FRIDAY, "11:00", "10:00");
        assertThrows(ValidationException.class, () -> service.add(admin, backwards));
        TimetableEntry zero = Fakes.entry(java, ravi, r101, "A", DayOfWeek.FRIDAY, "10:00", "10:00");
        assertThrows(ValidationException.class, () -> service.add(admin, zero));
    }

    @Test
    void missingFieldsAreRejected() {
        TimetableEntry noSection = newEntry();
        noSection.setSection(" ");
        assertThrows(ValidationException.class, () -> service.add(admin, noSection));
        TimetableEntry noRoom = newEntry();
        noRoom.setClassroom(null);
        assertThrows(ValidationException.class, () -> service.add(admin, noRoom));
        TimetableEntry noDay = newEntry();
        noDay.setDayOfWeek(null);
        assertThrows(ValidationException.class, () -> service.add(admin, noDay));
    }

    @Test
    void addChecksAgainstNoExistingEntryButUpdateExcludesItself() {
        service.add(admin, newEntry());
        assertEquals(0, timetableDao.lastExcludeId);

        TimetableEntry existing = newEntry();
        existing.setTimetableId(42);
        service.update(admin, existing);
        assertEquals(42, timetableDao.lastExcludeId); // its own old slot is not counted as a clash
        assertEquals(1, timetableDao.updated.size());
    }

    @Test
    void updateStillRejectsRealClashes() {
        TimetableEntry existing = newEntry();
        existing.setTimetableId(42);
        timetableDao.roomConflicts = List.of(newEntry());
        assertThrows(ConflictException.class, () -> service.update(admin, existing));
        assertTrue(timetableDao.updated.isEmpty());
    }

    @Test
    void onlyAdminMayChangeTheTimetable() {
        assertThrows(AccessDeniedException.class, () -> service.add(ravi, newEntry()));
        assertThrows(AccessDeniedException.class, () -> service.add(Fakes.student(1, 5, "Aarav"), newEntry()));
        assertThrows(AccessDeniedException.class, () -> service.delete(ravi, 1));
        assertTrue(timetableDao.inserted.isEmpty());
    }
}
