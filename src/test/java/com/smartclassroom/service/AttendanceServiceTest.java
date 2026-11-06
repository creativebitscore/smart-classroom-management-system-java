package com.smartclassroom.service;

import com.smartclassroom.model.AttendanceStatus;
import com.smartclassroom.model.AttendanceSummary;
import com.smartclassroom.model.Faculty;
import com.smartclassroom.model.Student;
import com.smartclassroom.model.Subject;
import com.smartclassroom.service.exception.AccessDeniedException;
import com.smartclassroom.service.exception.ConflictException;
import com.smartclassroom.service.exception.ValidationException;
import com.smartclassroom.util.DbConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttendanceServiceTest {
    private static final LocalDate PAST = LocalDate.now().minusDays(2);

    private Fakes.FakeAttendanceDao attendanceDao;
    private Fakes.FakeSettingsDao settingsDao;
    private AttendanceService service;
    private Faculty ravi;
    private Faculty anita;
    private final com.smartclassroom.model.Admin admin = Fakes.admin();

    @BeforeEach
    void setUp() {
        DbConnection.useTransactionRunner(new Fakes.DirectTransactions());
        attendanceDao = new Fakes.FakeAttendanceDao();
        settingsDao = new Fakes.FakeSettingsDao();
        Fakes.FakeSubjectDao subjectDao = new Fakes.FakeSubjectDao();
        Subject ds = Fakes.subject(1, "CS301", "Data Structures");
        subjectDao.subjects.put(1, ds);
        ravi = Fakes.faculty(1, 2, "Dr. Ravi Kumar");
        anita = Fakes.faculty(2, 3, "Prof. Anita Sharma");
        subjectDao.taught.put(1, List.of(ds)); // only Ravi teaches subject 1
        service = new AttendanceService(attendanceDao, new Fakes.FakeStudentDao(), subjectDao,
                new Fakes.FakeTimetableDao(), settingsDao);
    }

    @AfterEach
    void tearDown() {
        DbConnection.resetTransactionRunner();
    }

    private static Map<Integer, AttendanceStatus> sample() {
        Map<Integer, AttendanceStatus> map = new LinkedHashMap<>();
        map.put(1, AttendanceStatus.PRESENT);
        map.put(2, AttendanceStatus.ABSENT);
        map.put(3, AttendanceStatus.LATE);
        return map;
    }

    @Test
    void attendanceIsSavedForWholeClass() {
        service.markAttendance(ravi, 1, PAST, sample());
        assertEquals(3, attendanceDao.inserted.size());
        assertEquals(AttendanceStatus.LATE, attendanceDao.inserted.get(2).getStatus());
        assertEquals(1, attendanceDao.inserted.get(0).getMarkedBy()); // faculty id of the teacher
        assertEquals(PAST, attendanceDao.inserted.get(0).getDate());
    }

    @Test
    void duplicateAttendanceForSameStudentSubjectDateIsRejected() {
        attendanceDao.alreadyMarked = Set.of(2);
        ConflictException e = assertThrows(ConflictException.class, () -> service.markAttendance(ravi, 1, PAST, sample()));
        assertTrue(e.getMessage().contains("already recorded"));
        assertTrue(attendanceDao.inserted.isEmpty()); // nothing at all is saved
    }

    @Test
    void databaseUniqueKeyIsAlsoMappedToConflict() {
        // two teachers saving at the same moment: the pre-check passes but MySQL's UNIQUE key rejects the insert
        attendanceDao.failOnInsert = new SQLException(
                "Duplicate entry '1-1-2026-10-25' for key 'attendance.uq_attendance'", "23000", 1062);
        assertThrows(ConflictException.class, () -> service.markAttendance(ravi, 1, PAST, sample()));
    }

    @Test
    void futureDateIsRejected() {
        assertThrows(ValidationException.class,
                () -> service.markAttendance(ravi, 1, LocalDate.now().plusDays(1), sample()));
        assertTrue(attendanceDao.inserted.isEmpty());
    }

    @Test
    void emptyOrIncompleteInputIsRejected() {
        assertThrows(ValidationException.class, () -> service.markAttendance(ravi, 1, PAST, Map.of()));
        assertThrows(ValidationException.class, () -> service.markAttendance(ravi, 1, null, sample()));
        Map<Integer, AttendanceStatus> withNull = new LinkedHashMap<>();
        withNull.put(1, null);
        assertThrows(ValidationException.class, () -> service.markAttendance(ravi, 1, PAST, withNull));
    }

    @Test
    void facultyCannotMarkASubjectTheyDoNotTeach() {
        assertThrows(AccessDeniedException.class, () -> service.markAttendance(anita, 1, PAST, sample()));
        assertTrue(attendanceDao.inserted.isEmpty());
    }

    @Test
    void adminAndStudentCannotMarkAttendance() {
        assertThrows(AccessDeniedException.class, () -> service.markAttendance(admin, 1, PAST, sample()));
        Student student = Fakes.student(1, 5, "Aarav");
        assertThrows(AccessDeniedException.class, () -> service.markAttendance(student, 1, PAST, sample()));
    }

    @Test
    void correctingExistingAttendanceUpdatesInsteadOfDuplicating() {
        attendanceDao.alreadyMarked = Set.of(1, 2, 3);
        service.updateAttendance(ravi, 1, PAST, sample());
        assertEquals(3, attendanceDao.updated.size());
        assertTrue(attendanceDao.inserted.isEmpty());
    }

    @Test
    void updatingStudentsThatWereNeverMarkedIsRejected() {
        attendanceDao.alreadyMarked = Set.of(1);
        assertThrows(ValidationException.class, () -> service.updateAttendance(ravi, 1, PAST, sample()));
        assertTrue(attendanceDao.updated.isEmpty());
    }

    // ----- threshold and summaries -----

    @Test
    void thresholdDefaultsTo75AndAdminCanChangeIt() {
        assertEquals(75, service.getThreshold());
        service.setThreshold(admin, 80);
        assertEquals(80, service.getThreshold());
    }

    @Test
    void invalidThresholdAndNonAdminAreRejected() {
        assertThrows(ValidationException.class, () -> service.setThreshold(admin, 0));
        assertThrows(ValidationException.class, () -> service.setThreshold(admin, 101));
        assertThrows(AccessDeniedException.class, () -> service.setThreshold(ravi, 60));
        assertEquals(75, service.getThreshold());
    }

    @Test
    void corruptThresholdSettingFallsBackToDefault() {
        settingsDao.values.put(AttendanceService.THRESHOLD_KEY, "abc");
        assertEquals(75, service.getThreshold());
    }

    @Test
    void onlyStudentsBelowTheThresholdAreReported() {
        attendanceDao.summaries = List.of(
                new AttendanceSummary(1, "R1", "Aarav", "CS301", "DS", 4, 4, 0),   // 100%
                new AttendanceSummary(2, "R2", "Priya", "CS301", "DS", 4, 3, 0),   // 75% exactly: OK
                new AttendanceSummary(3, "R3", "Rahul", "CS301", "DS", 4, 2, 0));  // 50%: low
        List<AttendanceSummary> low = service.getBelowThreshold(admin, 1, null);
        assertEquals(1, low.size());
        assertEquals("Rahul", low.get(0).getStudentName());

        service.setThreshold(admin, 90);
        assertEquals(2, service.getBelowThreshold(admin, 1, null).size()); // threshold is configurable
    }

    @Test
    void facultyMustPickOwnSubjectToSeeSummaries() {
        assertThrows(ValidationException.class, () -> service.getClassSummaries(ravi, null, null, null, null));
        assertThrows(AccessDeniedException.class, () -> service.getClassSummaries(anita, 1, null, null, null));
        service.getClassSummaries(ravi, 1, "A", null, null);
        service.getClassSummaries(admin, null, null, null, null);
    }

    @Test
    void studentCannotSeeClassSummaries() {
        assertThrows(AccessDeniedException.class,
                () -> service.getClassSummaries(Fakes.student(1, 5, "Aarav"), 1, null, null, null));
    }

    @Test
    void reportDateRangeMustBeInOrder() {
        assertThrows(ValidationException.class, () -> service.getClassSummaries(admin, null, null,
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1)));
    }
}
