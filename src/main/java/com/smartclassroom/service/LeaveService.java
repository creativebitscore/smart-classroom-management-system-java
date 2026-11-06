package com.smartclassroom.service;

import com.smartclassroom.dao.LeaveRequestDao;
import com.smartclassroom.model.LeaveRequest;
import com.smartclassroom.model.RequestStatus;
import com.smartclassroom.model.Role;
import com.smartclassroom.model.Student;
import com.smartclassroom.model.User;
import com.smartclassroom.service.exception.AccessDeniedException;
import com.smartclassroom.service.exception.ConflictException;
import com.smartclassroom.service.exception.ValidationException;
import com.smartclassroom.util.Validator;

import java.time.LocalDate;
import java.util.List;

/**
 * Leave requests. Students apply and may cancel while the request is still pending; admins and faculty
 * approve or reject. Rules checked here (not in the screen):
 *  - the last day is not before the first day, and the leave is at most 30 days long
 *  - the leave may start at most 7 days in the past (for example sick leave applied after recovery)
 *  - a student cannot have two pending/approved requests that share a day
 *  - only a PENDING request can be decided or cancelled, and a rejection needs a written reason
 */
public class LeaveService {
    public static final int MAX_DAYS = 30;
    public static final int MAX_BACKDATE_DAYS = 7;

    private final LeaveRequestDao leaveDao;

    public LeaveService() {
        this(new LeaveRequestDao());
    }

    public LeaveService(LeaveRequestDao leaveDao) {
        this.leaveDao = leaveDao;
    }

    public List<LeaveRequest> getForStudent(User actor) {
        return leaveDao.findByStudent(requireStudent(actor).getStudentId());
    }

    /** All requests for admin/faculty, optionally only those with one status (null = every status). */
    public List<LeaveRequest> getForReview(User actor, RequestStatus onlyStatus) {
        AccessControl.requireRole(actor, Role.ADMIN, Role.FACULTY);
        return onlyStatus == null ? leaveDao.findAll() : leaveDao.findByStatus(onlyStatus);
    }

    public int countPending(User actor) {
        AccessControl.requireRole(actor, Role.ADMIN, Role.FACULTY);
        return leaveDao.countByStatus(RequestStatus.PENDING);
    }

    public void apply(User actor, LeaveRequest request) {
        Student student = requireStudent(actor);
        Validator.requireNotNull(request.getFromDate(), "the first day of leave");
        Validator.requireNotNull(request.getToDate(), "the last day of leave");
        request.setReason(Validator.requireText(request.getReason(), "Reason", 500));
        if (request.getToDate().isBefore(request.getFromDate())) {
            throw new ValidationException("The last day cannot be before the first day.");
        }
        if (request.getFromDate().isBefore(LocalDate.now().minusDays(MAX_BACKDATE_DAYS))) {
            throw new ValidationException("Leave can start at most " + MAX_BACKDATE_DAYS
                    + " days in the past. Please contact the office for older dates.");
        }
        if (request.getDays() > MAX_DAYS) {
            throw new ValidationException("A single request can cover at most " + MAX_DAYS + " days.");
        }
        List<LeaveRequest> overlapping =
                leaveDao.findOverlapping(student.getStudentId(), request.getFromDate(), request.getToDate());
        if (!overlapping.isEmpty()) {
            LeaveRequest other = overlapping.get(0);
            throw new ConflictException("You already have a " + other.getStatus().getDisplayName().toLowerCase()
                    + " leave request for " + other.getFromDate() + " to " + other.getToDate() + ".");
        }
        request.setStudentId(student.getStudentId());
        request.setStatus(RequestStatus.PENDING);
        request.setLeaveId(leaveDao.insert(request));
    }

    public void approve(User actor, int leaveId, String remark) {
        AccessControl.requireRole(actor, Role.ADMIN, Role.FACULTY);
        requirePending(leaveId);
        leaveDao.updateStatus(leaveId, RequestStatus.APPROVED, actor.getUserId(), optionalRemark(remark, "Approved"));
    }

    public void reject(User actor, int leaveId, String remark) {
        AccessControl.requireRole(actor, Role.ADMIN, Role.FACULTY);
        requirePending(leaveId);
        String reason = Validator.requireText(remark, "A reason for rejecting", 255);
        leaveDao.updateStatus(leaveId, RequestStatus.REJECTED, actor.getUserId(), reason);
    }

    /** A student withdraws his or her own request before it is decided. */
    public void cancel(User actor, int leaveId) {
        Student student = requireStudent(actor);
        LeaveRequest existing = requirePending(leaveId);
        if (existing.getStudentId() != student.getStudentId()) {
            throw new AccessDeniedException("You can only cancel your own leave requests.");
        }
        leaveDao.updateStatus(leaveId, RequestStatus.CANCELLED, null, "Cancelled by student");
    }

    private LeaveRequest requirePending(int leaveId) {
        LeaveRequest request = leaveDao.findById(leaveId)
                .orElseThrow(() -> new ValidationException("Leave request not found."));
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ConflictException("This request was already " + request.getStatus().getDisplayName().toLowerCase()
                    + ", so it cannot be changed.");
        }
        return request;
    }

    private static Student requireStudent(User actor) {
        AccessControl.requireRole(actor, Role.STUDENT);
        return (Student) actor;
    }

    private static String optionalRemark(String remark, String fallback) {
        if (remark == null || remark.isBlank()) {
            return fallback;
        }
        return Validator.requireText(remark, "Remark", 255);
    }
}
