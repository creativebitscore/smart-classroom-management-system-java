package com.smartclassroom.service;

import com.smartclassroom.model.Announcement;
import com.smartclassroom.model.Assignment;
import com.smartclassroom.model.AttendanceRecord;
import com.smartclassroom.model.AttendanceStatus;
import com.smartclassroom.model.Classroom;
import com.smartclassroom.model.Faculty;
import com.smartclassroom.model.Subject;
import com.smartclassroom.service.exception.AccessDeniedException;
import com.smartclassroom.service.exception.ValidationException;
import com.smartclassroom.util.DbConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Classroom, assignment, marks, announcement and report rules. */
class OtherServicesTest {
    private final com.smartclassroom.model.Admin admin = Fakes.admin();
    private Faculty ravi;
    private Faculty anita;
    private Fakes.FakeSubjectDao subjectDao;

    @BeforeEach
    void setUp() {
        DbConnection.useTransactionRunner(new Fakes.DirectTransactions());
        ravi = Fakes.faculty(1, 2, "Dr. Ravi Kumar");
        anita = Fakes.faculty(2, 3, "Prof. Anita Sharma");
        subjectDao = new Fakes.FakeSubjectDao();
        Subject ds = Fakes.subject(1, "CS301", "Data Structures");
        subjectDao.subjects.put(1, ds);
        subjectDao.taught.put(1, List.of(ds));
    }

    @AfterEach
    void tearDown() {
        DbConnection.resetTransactionRunner();
    }

    // ----- classrooms and capacity -----

    private static class RecordingClassroomDao extends com.smartclassroom.dao.ClassroomDao {
        final java.util.List<Classroom> inserted = new java.util.ArrayList<>();

        @Override
        public int insert(Classroom room) {
            inserted.add(room);
            return 7;
        }
    }

    private Classroom roomForm(String name, int capacity) {
        Classroom c = new Classroom();
        c.setRoomName(name);
        c.setCapacity(capacity);
        return c;
    }

    @Test
    void validClassroomIsSaved() {
        RecordingClassroomDao dao = new RecordingClassroomDao();
        ClassroomService service = new ClassroomService(dao);
        service.add(admin, roomForm(" R201 ", 45));
        assertEquals("R201", dao.inserted.get(0).getRoomName());
    }

    @Test
    void invalidClassroomsAreRejected() {
        RecordingClassroomDao dao = new RecordingClassroomDao();
        ClassroomService service = new ClassroomService(dao);
        assertThrows(ValidationException.class, () -> service.add(admin, roomForm("", 30)));
        Classroom tooBig = roomForm("R1", 30);
        tooBig.setCapacity(5000);
        assertThrows(ValidationException.class, () -> service.add(admin, tooBig));
        assertTrue(dao.inserted.isEmpty());
    }

    @Test
    void onlyAdminMayManageClassrooms() {
        ClassroomService service = new ClassroomService(new RecordingClassroomDao());
        assertThrows(AccessDeniedException.class, () -> service.add(ravi, roomForm("R9", 30)));
        assertThrows(AccessDeniedException.class, () -> service.delete(Fakes.student(1, 5, "A"), 1));
    }

    // ----- assignments -----

    private Assignment assignmentForm(String title, LocalDate due) {
        Assignment a = new Assignment();
        a.setSubjectId(1);
        a.setTitle(title);
        a.setDueDate(due);
        return a;
    }

    @Test
    void facultyPublishesAssignmentWithDeadline() {
        Fakes.FakeAssignmentDao dao = new Fakes.FakeAssignmentDao();
        AssignmentService service = new AssignmentService(dao, subjectDao);
        Assignment a = assignmentForm("  Linked lists ", LocalDate.now().plusDays(5));
        service.create(ravi, a);
        assertEquals(1, dao.inserted.size());
        assertEquals("Linked lists", a.getTitle());
        assertEquals(1, a.getFacultyId());
    }

    @Test
    void invalidAssignmentsAreRejected() {
        Fakes.FakeAssignmentDao dao = new Fakes.FakeAssignmentDao();
        AssignmentService service = new AssignmentService(dao, subjectDao);
        assertThrows(ValidationException.class, () -> service.create(ravi, assignmentForm("", LocalDate.now().plusDays(1))));
        assertThrows(ValidationException.class, () -> service.create(ravi, assignmentForm("T", null)));
        assertThrows(ValidationException.class, () -> service.create(ravi, assignmentForm("T", LocalDate.now().minusDays(1))));
        assertTrue(dao.inserted.isEmpty());
    }

    @Test
    void facultyCannotPublishForOrChangeOthersWork() {
        Fakes.FakeAssignmentDao dao = new Fakes.FakeAssignmentDao();
        AssignmentService service = new AssignmentService(dao, subjectDao);
        assertThrows(AccessDeniedException.class,
                () -> service.create(anita, assignmentForm("T", LocalDate.now().plusDays(1)))); // anita does not teach subject 1

        Assignment existing = assignmentForm("Old", LocalDate.now().plusDays(3));
        existing.setAssignmentId(5);
        existing.setFacultyId(1); // belongs to Ravi
        dao.byId.put(5, existing);
        assertThrows(AccessDeniedException.class, () -> service.delete(anita, 5));
        assertThrows(AccessDeniedException.class, () -> service.update(anita, existing));
        service.delete(ravi, 5);
        assertEquals(List.of(5), dao.deleted);
    }

    @Test
    void studentCannotCreateAssignments() {
        AssignmentService service = new AssignmentService(new Fakes.FakeAssignmentDao(), subjectDao);
        assertThrows(AccessDeniedException.class,
                () -> service.create(Fakes.student(1, 5, "A"), assignmentForm("T", LocalDate.now().plusDays(1))));
    }

    @Test
    void upcomingCountIgnoresOverdueAssignments() {
        AssignmentService service = new AssignmentService(new Fakes.FakeAssignmentDao(), subjectDao);
        LocalDate today = LocalDate.of(2026, 11, 10);
        long count = service.countUpcoming(List.of(
                assignmentForm("a", today.minusDays(1)), assignmentForm("b", today), assignmentForm("c", today.plusDays(2))), today);
        assertEquals(2, count);
    }

    // ----- marks -----

    @Test
    void marksAreSavedWhenValid() {
        Fakes.FakeMarkDao markDao = new Fakes.FakeMarkDao();
        MarkService service = new MarkService(markDao, new Fakes.FakeStudentDao(), subjectDao);
        service.saveMarks(ravi, 1, "mid1", 25, Map.of(1, 22.0, 2, 0.0, 3, 25.0));
        assertEquals(3, markDao.saved.size());
        assertEquals("MID1", markDao.saved.get(0).getExamType());
    }

    @Test
    void invalidMarksAreRejected() {
        Fakes.FakeMarkDao markDao = new Fakes.FakeMarkDao();
        MarkService service = new MarkService(markDao, new Fakes.FakeStudentDao(), subjectDao);
        assertThrows(ValidationException.class, () -> service.saveMarks(ravi, 1, "MID1", 25, Map.of(1, 26.0)));
        assertThrows(ValidationException.class, () -> service.saveMarks(ravi, 1, "MID1", 25, Map.of(1, -1.0)));
        assertThrows(ValidationException.class, () -> service.saveMarks(ravi, 1, "MID1", 0, Map.of(1, 0.0)));
        assertThrows(ValidationException.class, () -> service.saveMarks(ravi, 1, "", 25, Map.of(1, 5.0)));
        assertThrows(ValidationException.class, () -> service.saveMarks(ravi, 1, "MID1", 25, Map.of()));
        assertTrue(markDao.saved.isEmpty());
    }

    @Test
    void onlyTheTeacherOfTheSubjectMayEnterMarks() {
        Fakes.FakeMarkDao markDao = new Fakes.FakeMarkDao();
        MarkService service = new MarkService(markDao, new Fakes.FakeStudentDao(), subjectDao);
        assertThrows(AccessDeniedException.class, () -> service.saveMarks(anita, 1, "MID1", 25, Map.of(1, 5.0)));
        assertThrows(AccessDeniedException.class, () -> service.saveMarks(admin, 1, "MID1", 25, Map.of(1, 5.0)));
        assertTrue(markDao.saved.isEmpty());
    }

    // ----- announcements -----

    @Test
    void adminAndFacultyCanPostButStudentsCannot() {
        Fakes.FakeAnnouncementDao dao = new Fakes.FakeAnnouncementDao();
        AnnouncementService service = new AnnouncementService(dao);
        service.post(admin, "Holiday", "College closed on Friday.", Announcement.Audience.ALL);
        service.post(ravi, "Lab", "Bring records.", Announcement.Audience.STUDENTS);
        assertEquals(2, dao.inserted.size());
        assertThrows(AccessDeniedException.class,
                () -> service.post(Fakes.student(1, 5, "A"), "Hi", "Hello", Announcement.Audience.ALL));
    }

    @Test
    void announcementFieldsAreValidated() {
        AnnouncementService service = new AnnouncementService(new Fakes.FakeAnnouncementDao());
        assertThrows(ValidationException.class, () -> service.post(admin, "", "text", Announcement.Audience.ALL));
        assertThrows(ValidationException.class, () -> service.post(admin, "Title", "  ", Announcement.Audience.ALL));
        assertThrows(ValidationException.class, () -> service.post(admin, "Title", "text", null));
    }

    @Test
    void facultyCanDeleteOnlyTheirOwnAnnouncements() {
        Fakes.FakeAnnouncementDao dao = new Fakes.FakeAnnouncementDao();
        AnnouncementService service = new AnnouncementService(dao);
        Announcement mine = new Announcement();
        mine.setAnnouncementId(1);
        mine.setPostedBy(ravi.getUserId());
        Announcement others = new Announcement();
        others.setAnnouncementId(2);
        others.setPostedBy(anita.getUserId());
        service.delete(ravi, mine);
        assertThrows(AccessDeniedException.class, () -> service.delete(ravi, others));
        service.delete(admin, others); // admin may delete any
        assertEquals(List.of(1, 2), dao.deleted);
    }

    // ----- report export -----

    @Test
    void attendanceReportExportsToCsv(@TempDir Path folder) throws IOException {
        ReportService reports = new ReportService();
        AttendanceRecord r = new AttendanceRecord(1, 1, LocalDate.of(2026, 10, 19), AttendanceStatus.LATE, 1);
        r.setRollNo("CSE2301");
        r.setStudentName("Aarav Mehta");
        r.setSubjectName("Data Structures");
        Path file = folder.resolve("report.csv");
        reports.exportAttendanceRecords(file, List.of(r));
        String text = Files.readString(file);
        assertTrue(text.contains("Date,Roll No,Student,Subject,Status"));
        assertTrue(text.contains("2026-10-19,CSE2301,Aarav Mehta,Data Structures,Late"));
    }
}
