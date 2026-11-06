package com.smartclassroom.service;

import com.smartclassroom.model.Classroom;
import com.smartclassroom.model.Faculty;
import com.smartclassroom.model.RequestStatus;
import com.smartclassroom.model.RoomBooking;
import com.smartclassroom.model.Student;
import com.smartclassroom.model.Subject;
import com.smartclassroom.service.exception.AccessDeniedException;
import com.smartclassroom.service.exception.ConflictException;
import com.smartclassroom.service.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Room booking rules: slot validation, capacity, clashes with the timetable and other bookings, approval. */
class RoomBookingServiceTest {
    private final com.smartclassroom.model.Admin admin = Fakes.admin();
    private Faculty ravi;
    private Faculty anita;
    private Student student;
    private Fakes.FakeBookingDao bookingDao;
    private Fakes.FakeTimetableDao timetableDao;
    private Fakes.FakeClassroomDao classroomDao;
    private RoomBookingService service;
    private final LocalDate tomorrow = LocalDate.now().plusDays(1);

    @BeforeEach
    void setUp() {
        ravi = Fakes.faculty(1, 2, "Dr. Ravi Kumar");
        anita = Fakes.faculty(2, 3, "Prof. Anita Sharma");
        student = Fakes.student(1, 5, "Aarav");
        bookingDao = new Fakes.FakeBookingDao();
        timetableDao = new Fakes.FakeTimetableDao();
        classroomDao = new Fakes.FakeClassroomDao();
        classroomDao.rooms.put(1, Fakes.room(1, "R101", 60));
        classroomDao.rooms.put(2, Fakes.room(2, "R102", 40));
        classroomDao.rooms.put(3, Fakes.room(3, "LAB-1", 30));
        service = new RoomBookingService(bookingDao, timetableDao, classroomDao);
    }

    private RoomBooking booking(int roomId, int people) {
        RoomBooking b = new RoomBooking();
        b.setClassroomId(roomId);
        b.setPurpose("Extra class");
        b.setBookingDate(tomorrow);
        b.setStartTime(LocalTime.of(14, 0));
        b.setEndTime(LocalTime.of(15, 0));
        b.setExpectedAttendees(people);
        return b;
    }

    private com.smartclassroom.model.TimetableEntry regularClass() {
        Subject ds = Fakes.subject(1, "CS301", "Data Structures");
        return Fakes.entry(ds, ravi, Fakes.room(1, "R101", 60), "A", DayOfWeek.MONDAY, "14:00", "15:00");
    }

    private RoomBooking stored(int id, Faculty owner, RequestStatus status, LocalDate date) {
        RoomBooking b = booking(1, 30);
        b.setBookingId(id);
        b.setFacultyId(owner.getFacultyId());
        b.setStatus(status);
        b.setBookingDate(date);
        b.setRoomName("R101");
        bookingDao.byId.put(id, b);
        return b;
    }

    // ----- requesting -----

    @Test
    void facultyCanRequestAFreeRoomAndItStartsPending() {
        RoomBooking b = booking(1, 40);
        service.request(ravi, b);
        assertEquals(1, bookingDao.inserted.size());
        assertEquals(RequestStatus.PENDING, b.getStatus());
        assertEquals(ravi.getFacultyId(), b.getFacultyId());
    }

    @Test
    void onlyFacultyCanRequest() {
        assertThrows(AccessDeniedException.class, () -> service.request(student, booking(1, 10)));
        assertThrows(AccessDeniedException.class, () -> service.request(admin, booking(1, 10)));
    }

    @Test
    void rejectsPastDateBadTimesAndMissingFields() {
        RoomBooking past = booking(1, 10);
        past.setBookingDate(LocalDate.now().minusDays(1));
        assertThrows(ValidationException.class, () -> service.request(ravi, past));

        RoomBooking backwards = booking(1, 10);
        backwards.setStartTime(LocalTime.of(16, 0));
        backwards.setEndTime(LocalTime.of(15, 0));
        assertThrows(ValidationException.class, () -> service.request(ravi, backwards));

        RoomBooking noPurpose = booking(1, 10);
        noPurpose.setPurpose("  ");
        assertThrows(ValidationException.class, () -> service.request(ravi, noPurpose));

        assertThrows(ValidationException.class, () -> service.request(ravi, booking(0, 10)));
        assertThrows(ValidationException.class, () -> service.request(ravi, booking(99, 10)));
        assertThrows(ValidationException.class, () -> service.request(ravi, booking(1, 0)));
        assertTrue(bookingDao.inserted.isEmpty());
    }

    @Test
    void groupLargerThanTheRoomIsRefused() {
        ValidationException e = assertThrows(ValidationException.class, () -> service.request(ravi, booking(3, 45)));
        assertTrue(e.getMessage().contains("LAB-1"));
        service.request(ravi, booking(3, 30)); // exactly the capacity is fine
        assertEquals(1, bookingDao.inserted.size());
    }

    @Test
    void roomWithARegularClassAtThatTimeIsRefused() {
        timetableDao.roomConflicts.add(regularClass());
        ConflictException e = assertThrows(ConflictException.class, () -> service.request(ravi, booking(1, 20)));
        assertTrue(e.getMessage().contains("regular class"));
        assertTrue(bookingDao.inserted.isEmpty());
    }

    @Test
    void roomAlreadyBookedAndApprovedIsRefused() {
        bookingDao.approvedConflicts.add(stored(5, anita, RequestStatus.APPROVED, tomorrow));
        ConflictException e = assertThrows(ConflictException.class, () -> service.request(ravi, booking(1, 20)));
        assertTrue(e.getMessage().contains("already booked"));
    }

    @Test
    void facultyCannotBookWhileTeachingARegularClass() {
        timetableDao.facultyConflicts.add(regularClass());
        ConflictException e = assertThrows(ConflictException.class, () -> service.request(ravi, booking(2, 20)));
        assertTrue(e.getMessage().contains("teaching"));
    }

    // ----- finding free rooms -----

    @Test
    void freeRoomSearchSkipsRoomsThatAreTooSmall() {
        List<Classroom> free = service.findAvailableRooms(ravi, tomorrow, LocalTime.of(14, 0), LocalTime.of(15, 0), 35);
        assertEquals(List.of("R101", "R102"), free.stream().map(Classroom::getRoomName).toList());
    }

    @Test
    void freeRoomSearchSkipsRoomsWithClassesOrBookings() {
        timetableDao.roomConflicts.add(regularClass()); // the fake reports the same clash for every room
        assertTrue(service.findAvailableRooms(ravi, tomorrow, LocalTime.of(14, 0), LocalTime.of(15, 0), 10).isEmpty());
    }

    @Test
    void freeRoomSearchIsEmptyWhenTheFacultyIsTeaching() {
        timetableDao.facultyConflicts.add(regularClass());
        assertTrue(service.findAvailableRooms(ravi, tomorrow, LocalTime.of(14, 0), LocalTime.of(15, 0), 10).isEmpty());
    }

    // ----- deciding -----

    @Test
    void adminApprovesAPendingBooking() {
        stored(1, ravi, RequestStatus.PENDING, tomorrow);
        service.approve(admin, 1, "");
        assertEquals("1:APPROVED:Approved", bookingDao.updates.get(0));
        assertEquals(1, bookingDao.lastExcludeId); // its own pending row must not count as a clash
    }

    @Test
    void approvalChecksTheRoomAgain() {
        stored(1, ravi, RequestStatus.PENDING, tomorrow);
        bookingDao.approvedConflicts.add(stored(2, anita, RequestStatus.APPROVED, tomorrow)); // approved meanwhile
        assertThrows(ConflictException.class, () -> service.approve(admin, 1, ""));
        assertTrue(bookingDao.updates.isEmpty());
    }

    @Test
    void approvalOfAPastDateIsRefused() {
        stored(1, ravi, RequestStatus.PENDING, LocalDate.now().minusDays(1));
        assertThrows(ValidationException.class, () -> service.approve(admin, 1, ""));
    }

    @Test
    void onlyTheAdminDecides() {
        stored(1, ravi, RequestStatus.PENDING, tomorrow);
        assertThrows(AccessDeniedException.class, () -> service.approve(anita, 1, ""));
        assertThrows(AccessDeniedException.class, () -> service.reject(ravi, 1, "No"));
        assertThrows(AccessDeniedException.class, () -> service.getAll(ravi));
        assertTrue(bookingDao.updates.isEmpty());
    }

    @Test
    void rejectionNeedsAReasonAndOnlyPendingCanBeDecided() {
        stored(1, ravi, RequestStatus.PENDING, tomorrow);
        stored(2, ravi, RequestStatus.APPROVED, tomorrow);
        assertThrows(ValidationException.class, () -> service.reject(admin, 1, " "));
        service.reject(admin, 1, "Room under maintenance");
        assertEquals("1:REJECTED:Room under maintenance", bookingDao.updates.get(0));
        assertThrows(ConflictException.class, () -> service.reject(admin, 2, "Too late"));
        assertThrows(ConflictException.class, () -> service.approve(admin, 2, ""));
    }

    // ----- cancelling -----

    @Test
    void ownerCancelsPendingOrApprovedFutureBooking() {
        stored(1, ravi, RequestStatus.PENDING, tomorrow);
        stored(2, ravi, RequestStatus.APPROVED, tomorrow);
        service.cancel(ravi, 1);
        service.cancel(ravi, 2);
        assertEquals("1:CANCELLED:Cancelled by faculty", bookingDao.updates.get(0));
        assertEquals("2:CANCELLED:Cancelled by faculty", bookingDao.updates.get(1));
    }

    @Test
    void cannotCancelOthersRejectedOrPastBookings() {
        stored(1, ravi, RequestStatus.PENDING, tomorrow);
        stored(2, ravi, RequestStatus.REJECTED, tomorrow);
        stored(3, ravi, RequestStatus.APPROVED, LocalDate.now().minusDays(2));
        assertThrows(AccessDeniedException.class, () -> service.cancel(anita, 1));
        assertThrows(ConflictException.class, () -> service.cancel(ravi, 2));
        assertThrows(ValidationException.class, () -> service.cancel(ravi, 3));
        assertTrue(bookingDao.updates.isEmpty());
    }
}
