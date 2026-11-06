package com.smartclassroom.service;

import com.smartclassroom.dao.ClassroomDao;
import com.smartclassroom.dao.RoomBookingDao;
import com.smartclassroom.dao.TimetableDao;
import com.smartclassroom.model.Classroom;
import com.smartclassroom.model.Faculty;
import com.smartclassroom.model.RequestStatus;
import com.smartclassroom.model.Role;
import com.smartclassroom.model.RoomBooking;
import com.smartclassroom.model.TimetableEntry;
import com.smartclassroom.model.User;
import com.smartclassroom.service.exception.AccessDeniedException;
import com.smartclassroom.service.exception.ConflictException;
import com.smartclassroom.service.exception.ValidationException;
import com.smartclassroom.util.Validator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One-off room bookings (extra class, doubt session, event). Faculty request, the admin approves.
 * A request is only possible, and only approvable, when ALL of these hold:
 *  - the date is today or later, the end time is after the start time, the purpose is given
 *  - the expected attendees fit in the room (capacity validation)
 *  - the room has no regular timetable class at that weekday and time
 *  - the room has no other APPROVED booking that overlaps on that date
 *  - the requesting faculty member is not teaching a regular class at that time
 * The checks run again when the admin approves, because another booking may have been approved meanwhile.
 */
public class RoomBookingService {
    private final RoomBookingDao bookingDao;
    private final TimetableDao timetableDao;
    private final ClassroomDao classroomDao;

    public RoomBookingService() {
        this(new RoomBookingDao(), new TimetableDao(), new ClassroomDao());
    }

    public RoomBookingService(RoomBookingDao bookingDao, TimetableDao timetableDao, ClassroomDao classroomDao) {
        this.bookingDao = bookingDao;
        this.timetableDao = timetableDao;
        this.classroomDao = classroomDao;
    }

    public List<RoomBooking> getForFaculty(User actor) {
        return bookingDao.findByFaculty(requireFaculty(actor).getFacultyId());
    }

    public List<RoomBooking> getAll(User actor) {
        AccessControl.requireRole(actor, Role.ADMIN);
        return bookingDao.findAll();
    }

    public int countPending(User actor) {
        AccessControl.requireRole(actor, Role.ADMIN);
        return bookingDao.countByStatus(RequestStatus.PENDING);
    }

    /** Rooms that are big enough and completely free at that date and time (used by the "Find free rooms" button). */
    public List<Classroom> findAvailableRooms(User actor, LocalDate date, LocalTime start, LocalTime end,
                                              int attendees) {
        Faculty faculty = requireFaculty(actor);
        validateSlot(date, start, end);
        if (attendees <= 0) {
            throw new ValidationException("Expected attendees must be at least 1.");
        }
        List<Classroom> free = new ArrayList<>();
        for (Classroom room : classroomDao.findAll()) {
            if (room.getCapacity() >= attendees && roomProblem(room, date, start, end, 0) == null) {
                free.add(room);
            }
        }
        // the faculty member must be free too, otherwise no room helps
        if (facultyProblem(faculty, date, start, end) != null) {
            return List.of();
        }
        return free;
    }

    public void request(User actor, RoomBooking booking) {
        Faculty faculty = requireFaculty(actor);
        booking.setPurpose(Validator.requireText(booking.getPurpose(), "Purpose", 150));
        validateSlot(booking.getBookingDate(), booking.getStartTime(), booking.getEndTime());
        if (booking.getClassroomId() <= 0) {
            throw new ValidationException("Please select a room.");
        }
        Classroom room = classroomDao.findById(booking.getClassroomId())
                .orElseThrow(() -> new ValidationException("Room not found."));
        if (booking.getExpectedAttendees() <= 0) {
            throw new ValidationException("Expected attendees must be at least 1.");
        }
        if (booking.getExpectedAttendees() > room.getCapacity()) {
            throw new ValidationException("Room " + room.getRoomName() + " seats " + room.getCapacity()
                    + " but you expect " + booking.getExpectedAttendees() + " people. Choose a larger room.");
        }
        throwIfProblem(roomProblem(room, booking.getBookingDate(), booking.getStartTime(), booking.getEndTime(), 0));
        throwIfProblem(facultyProblem(faculty, booking.getBookingDate(), booking.getStartTime(), booking.getEndTime()));
        booking.setFacultyId(faculty.getFacultyId());
        booking.setStatus(RequestStatus.PENDING);
        booking.setBookingId(bookingDao.insert(booking));
    }

    public void approve(User actor, int bookingId, String remark) {
        AccessControl.requireRole(actor, Role.ADMIN);
        RoomBooking booking = requirePending(bookingId);
        if (booking.getBookingDate().isBefore(LocalDate.now())) {
            throw new ValidationException("This booking date has already passed.");
        }
        Classroom room = classroomDao.findById(booking.getClassroomId())
                .orElseThrow(() -> new ValidationException("The room no longer exists."));
        throwIfProblem(roomProblem(room, booking.getBookingDate(), booking.getStartTime(), booking.getEndTime(),
                booking.getBookingId()));
        bookingDao.updateStatus(bookingId, RequestStatus.APPROVED,
                remark == null || remark.isBlank() ? "Approved" : Validator.requireText(remark, "Remark", 255));
    }

    public void reject(User actor, int bookingId, String remark) {
        AccessControl.requireRole(actor, Role.ADMIN);
        requirePending(bookingId);
        bookingDao.updateStatus(bookingId, RequestStatus.REJECTED,
                Validator.requireText(remark, "A reason for rejecting", 255));
    }

    /** A faculty member withdraws his or her own pending or approved booking (not one in the past). */
    public void cancel(User actor, int bookingId) {
        Faculty faculty = requireFaculty(actor);
        RoomBooking booking = bookingDao.findById(bookingId)
                .orElseThrow(() -> new ValidationException("Booking not found."));
        if (booking.getFacultyId() != faculty.getFacultyId()) {
            throw new AccessDeniedException("You can only cancel your own bookings.");
        }
        if (booking.getStatus() != RequestStatus.PENDING && booking.getStatus() != RequestStatus.APPROVED) {
            throw new ConflictException("This booking is already " + booking.getStatus().getDisplayName().toLowerCase()
                    + ", so it cannot be cancelled.");
        }
        if (booking.getBookingDate().isBefore(LocalDate.now())) {
            throw new ValidationException("A booking in the past cannot be cancelled.");
        }
        bookingDao.updateStatus(bookingId, RequestStatus.CANCELLED, "Cancelled by faculty");
    }

    // ---------- rules ----------

    private static void validateSlot(LocalDate date, LocalTime start, LocalTime end) {
        Validator.requireNotNull(date, "a date");
        Validator.requireNotNull(start, "a start time");
        Validator.requireNotNull(end, "an end time");
        if (date.isBefore(LocalDate.now())) {
            throw new ValidationException("The date cannot be in the past.");
        }
        if (!start.isBefore(end)) {
            throw new ValidationException("End time must be after start time.");
        }
    }

    /** Returns a sentence describing why the room cannot be used, or null if it is free. */
    private String roomProblem(Classroom room, LocalDate date, LocalTime start, LocalTime end, int excludeBookingId) {
        List<TimetableEntry> classes = timetableDao.findRoomConflicts(
                room.getClassroomId(), date.getDayOfWeek(), start, end, 0);
        if (!classes.isEmpty()) {
            return "Room " + room.getRoomName() + " has a regular class then: " + describe(classes.get(0));
        }
        List<RoomBooking> others = bookingDao.findApprovedConflicts(
                room.getClassroomId(), date, start, end, excludeBookingId);
        if (!others.isEmpty()) {
            RoomBooking other = others.get(0);
            return "Room " + room.getRoomName() + " is already booked on " + date + " " + other.getSlotLabel()
                    + " (" + other.getPurpose() + ").";
        }
        return null;
    }

    private String facultyProblem(Faculty faculty, LocalDate date, LocalTime start, LocalTime end) {
        List<TimetableEntry> teaching = timetableDao.findFacultyConflicts(
                faculty.getFacultyId(), date.getDayOfWeek(), start, end, 0);
        if (!teaching.isEmpty()) {
            return "You are teaching a regular class at that time: " + describe(teaching.get(0));
        }
        return null;
    }

    private static void throwIfProblem(String problem) {
        if (problem != null) {
            throw new ConflictException(problem);
        }
    }

    private RoomBooking requirePending(int bookingId) {
        RoomBooking booking = bookingDao.findById(bookingId)
                .orElseThrow(() -> new ValidationException("Booking not found."));
        if (booking.getStatus() != RequestStatus.PENDING) {
            throw new ConflictException("This request was already " + booking.getStatus().getDisplayName().toLowerCase()
                    + ", so it cannot be changed.");
        }
        return booking;
    }

    private static Faculty requireFaculty(User actor) {
        AccessControl.requireRole(actor, Role.FACULTY);
        return (Faculty) actor;
    }

    private static String describe(TimetableEntry t) {
        return t.getSubject().getSubjectCode() + " (" + t.getDayOfWeek() + " " + t.getSlotLabel()
                + ", section " + t.getSection() + ")";
    }
}
