package com.smartclassroom.service;

import com.smartclassroom.model.Faculty;
import com.smartclassroom.model.LeaveRequest;
import com.smartclassroom.model.RequestStatus;
import com.smartclassroom.model.Student;
import com.smartclassroom.service.exception.AccessDeniedException;
import com.smartclassroom.service.exception.ConflictException;
import com.smartclassroom.service.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Leave request rules: validation, overlap, who may decide, and the PENDING-only life cycle. */
class LeaveServiceTest {
    private final com.smartclassroom.model.Admin admin = Fakes.admin();
    private Faculty ravi;
    private Student aarav;
    private Student priya;
    private Fakes.FakeLeaveDao dao;
    private LeaveService service;

    @BeforeEach
    void setUp() {
        ravi = Fakes.faculty(1, 2, "Dr. Ravi Kumar");
        aarav = Fakes.student(1, 5, "Aarav Mehta");
        priya = Fakes.student(2, 6, "Priya Nair");
        dao = new Fakes.FakeLeaveDao();
        service = new LeaveService(dao);
    }

    private static LeaveRequest request(LocalDate from, LocalDate to, String reason) {
        LeaveRequest r = new LeaveRequest();
        r.setFromDate(from);
        r.setToDate(to);
        r.setReason(reason);
        return r;
    }

    private LeaveRequest stored(int id, Student owner, RequestStatus status) {
        LeaveRequest r = request(LocalDate.now().plusDays(2), LocalDate.now().plusDays(3), "Fever");
        r.setLeaveId(id);
        r.setStudentId(owner.getStudentId());
        r.setStatus(status);
        dao.byId.put(id, r);
        return r;
    }

    // ----- applying -----

    @Test
    void studentCanApplyForLeaveAndItStartsPending() {
        LeaveRequest r = request(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), "  Fever  ");
        service.apply(aarav, r);
        assertEquals(1, dao.inserted.size());
        assertEquals(RequestStatus.PENDING, r.getStatus());
        assertEquals(1, r.getStudentId());
        assertEquals("Fever", r.getReason());
        assertEquals(2, r.getDays());
    }

    @Test
    void oneDayLeaveCountsAsOneDay() {
        LocalDate day = LocalDate.now().plusDays(1);
        assertEquals(1, request(day, day, "x").getDays());
    }

    @Test
    void onlyStudentsCanApply() {
        LeaveRequest r = request(LocalDate.now(), LocalDate.now(), "Fever");
        assertThrows(AccessDeniedException.class, () -> service.apply(ravi, r));
        assertThrows(AccessDeniedException.class, () -> service.apply(admin, r));
        assertThrows(AccessDeniedException.class, () -> service.apply(null, r));
    }

    @Test
    void rejectsMissingDatesAndReason() {
        LocalDate today = LocalDate.now();
        assertThrows(ValidationException.class, () -> service.apply(aarav, request(null, today, "x")));
        assertThrows(ValidationException.class, () -> service.apply(aarav, request(today, null, "x")));
        assertThrows(ValidationException.class, () -> service.apply(aarav, request(today, today, "   ")));
        assertThrows(ValidationException.class, () -> service.apply(aarav, request(today, today, "x".repeat(501))));
        assertTrue(dao.inserted.isEmpty());
    }

    @Test
    void lastDayCannotBeBeforeFirstDay() {
        LocalDate today = LocalDate.now();
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.apply(aarav, request(today.plusDays(3), today.plusDays(1), "Trip")));
        assertTrue(e.getMessage().contains("last day"));
    }

    @Test
    void leaveCannotStartMoreThanAWeekInThePast() {
        LocalDate today = LocalDate.now();
        service.apply(aarav, request(today.minusDays(LeaveService.MAX_BACKDATE_DAYS), today, "Sick, recovered"));
        assertThrows(ValidationException.class, () -> service.apply(aarav,
                request(today.minusDays(LeaveService.MAX_BACKDATE_DAYS + 1), today, "Too old")));
    }

    @Test
    void leaveCannotBeLongerThanThirtyDays() {
        LocalDate start = LocalDate.now().plusDays(1);
        service.apply(aarav, request(start, start.plusDays(LeaveService.MAX_DAYS - 1), "Exactly 30 days"));
        assertThrows(ValidationException.class, () -> service.apply(aarav,
                request(start, start.plusDays(LeaveService.MAX_DAYS), "31 days")));
    }

    @Test
    void overlappingRequestIsRefused() {
        LeaveRequest existing = stored(1, aarav, RequestStatus.APPROVED);
        dao.overlapping.add(existing);
        ConflictException e = assertThrows(ConflictException.class, () -> service.apply(aarav,
                request(LocalDate.now().plusDays(3), LocalDate.now().plusDays(5), "Another")));
        assertTrue(e.getMessage().contains("approved"));
        assertTrue(dao.inserted.isEmpty());
    }

    // ----- deciding -----

    @Test
    void adminAndFacultyCanApproveAndTheReviewerIsRecorded() {
        stored(1, aarav, RequestStatus.PENDING);
        stored(2, priya, RequestStatus.PENDING);
        service.approve(admin, 1, "  ");
        service.approve(ravi, 2, "Get well soon");
        assertEquals("1:APPROVED:1:Approved", dao.updates.get(0));
        assertEquals("2:APPROVED:" + ravi.getUserId() + ":Get well soon", dao.updates.get(1));
    }

    @Test
    void studentsCannotDecide() {
        stored(1, aarav, RequestStatus.PENDING);
        assertThrows(AccessDeniedException.class, () -> service.approve(priya, 1, ""));
        assertThrows(AccessDeniedException.class, () -> service.reject(priya, 1, "No"));
        assertTrue(dao.updates.isEmpty());
    }

    @Test
    void rejectionNeedsAReason() {
        stored(1, aarav, RequestStatus.PENDING);
        assertThrows(ValidationException.class, () -> service.reject(admin, 1, "  "));
        assertThrows(ValidationException.class, () -> service.reject(admin, 1, null));
        service.reject(admin, 1, "Exams are running");
        assertEquals("1:REJECTED:1:Exams are running", dao.updates.get(0));
    }

    @Test
    void aDecidedRequestCannotBeDecidedAgain() {
        stored(1, aarav, RequestStatus.REJECTED);
        stored(2, aarav, RequestStatus.APPROVED);
        assertThrows(ConflictException.class, () -> service.approve(admin, 1, ""));
        assertThrows(ConflictException.class, () -> service.reject(admin, 2, "Changed my mind"));
        assertTrue(dao.updates.isEmpty());
    }

    @Test
    void unknownRequestGivesAFriendlyError() {
        assertThrows(ValidationException.class, () -> service.approve(admin, 99, ""));
    }

    // ----- cancelling -----

    @Test
    void studentCanCancelOwnPendingRequest() {
        stored(1, aarav, RequestStatus.PENDING);
        service.cancel(aarav, 1);
        assertEquals("1:CANCELLED:null:Cancelled by student", dao.updates.get(0));
    }

    @Test
    void studentCannotCancelSomeoneElsesOrADecidedRequest() {
        stored(1, aarav, RequestStatus.PENDING);
        stored(2, aarav, RequestStatus.APPROVED);
        assertThrows(AccessDeniedException.class, () -> service.cancel(priya, 1));
        assertThrows(ConflictException.class, () -> service.cancel(aarav, 2));
        assertThrows(AccessDeniedException.class, () -> service.cancel(ravi, 1));
        assertTrue(dao.updates.isEmpty());
    }

    // ----- reading -----

    @Test
    void reviewListIsForStaffOnly() {
        assertThrows(AccessDeniedException.class, () -> service.getForReview(aarav, null));
        assertThrows(AccessDeniedException.class, () -> service.countPending(aarav));
        assertThrows(AccessDeniedException.class, () -> service.getForStudent(ravi));
    }
}
